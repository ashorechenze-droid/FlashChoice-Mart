package com.example.fcmseckillmicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmcommon.util.JsonUtil;
import com.example.fcmseckillmicroservice.feign.CollectNoticeFeignClient;
import com.example.fcmseckillmicroservice.feign.GoodFeignClient;
import com.example.fcmseckillmicroservice.feign.OrderFeignClient;
import com.example.fcmseckillmicroservice.config.SeckillRabbitConfig;
import com.example.fcmseckillmicroservice.mapper.SeckillMapper;
import com.example.fcmseckillmicroservice.service.SeckillReservationService;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.GrabResult;
import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.SeckillGrabEntity;
import com.example.fcmspi.model.SeckillOrderParam;
import com.example.fcmspi.model.SeckillEntity;
import com.example.fcmspi.model.SeckillGoodEntity;
import com.example.fcmspi.model.SeckillJoinNoticeParam;
import com.example.fcmspi.model.search.SeckillSearchBean;
import com.example.fcmspi.service.SeckillService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    /** 前台进行中活动列表缓存 key（秒杀会场首页高频访问） */
    private static final String CACHE_KEY_ACTIVE = "fcm:cache:seckill:active";
    /** 缓存有效期（活动开始/结束状态随时间推移变化，短 TTL + 管理端变更主动失效） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>(
            "local s=tonumber(redis.call('get',KEYS[1]) or '-1'); " +
            "if s < 0 then return -1 end; " +
            "if tonumber(redis.call('get',KEYS[2]) or '0') >= tonumber(ARGV[1]) then return -3 end; " +
            "if s == 0 then return -2 end; " +
            "redis.call('decr',KEYS[1]); redis.call('incr',KEYS[2]); redis.call('expire',KEYS[2],ARGV[2]); return 1", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists',KEYS[1]) == 1 then redis.call('incr',KEYS[1]) end; " +
            "local n=tonumber(redis.call('get',KEYS[2]) or '0'); " +
            "if n <= 1 then redis.call('del',KEYS[2]) else redis.call('decr',KEYS[2]) end; return 1", Long.class);

    private final SeckillMapper seckillMapper;
    private final GoodFeignClient goodFeignClient;
    private final StringRedisTemplate redisTemplate;
    private final CollectNoticeFeignClient collectNoticeFeignClient;
    private final RabbitTemplate rabbitTemplate;
    private final OrderFeignClient orderFeignClient;
    private final SeckillReservationService reservationService;

    public SeckillServiceImpl(SeckillMapper seckillMapper,
                              GoodFeignClient goodFeignClient,
                              StringRedisTemplate redisTemplate,
                              CollectNoticeFeignClient collectNoticeFeignClient,
                              RabbitTemplate rabbitTemplate,
                              OrderFeignClient orderFeignClient,
                              SeckillReservationService reservationService) {
        this.seckillMapper = seckillMapper;
        this.goodFeignClient = goodFeignClient;
        this.redisTemplate = redisTemplate;
        this.collectNoticeFeignClient = collectNoticeFeignClient;
        this.rabbitTemplate = rabbitTemplate;
        this.orderFeignClient = orderFeignClient;
        this.reservationService = reservationService;
    }

    /* ==================== 秒杀活动管理 ==================== */

    @Override
    public PageInfo<SeckillEntity> page(SeckillSearchBean search, PaginateInfo page) {
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        return new PageInfo<>(seckillMapper.findAll(search));
    }

    @Override
    public SeckillEntity getById(Long id) {
        return seckillMapper.findById(id);
    }

    @Override
    public int insert(SeckillEntity seckill) {
        if (seckill == null || isBlank(seckill.getName())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动名称不能为空");
        }
        if (isBlank(seckill.getStartTime()) || isBlank(seckill.getEndTime())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动起止时间不能为空");
        }
        if (seckill.getStartTime().compareTo(seckill.getEndTime()) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动开始时间不能晚于结束时间");
        }
        if (seckill.getEnabled() == null) {
            // 新活动默认启用
            seckill.setEnabled(true);
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        seckill.setCreatedTime(now);
        seckill.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        int rows = seckillMapper.save(seckill);
        evictActiveCache();
        return rows;
    }

    @Override
    public int update(SeckillEntity seckill) {
        if (seckill == null || seckill.getId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少活动 id");
        }
        if (isBlank(seckill.getName())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动名称不能为空");
        }
        if (!isBlank(seckill.getStartTime()) && !isBlank(seckill.getEndTime())
                && seckill.getStartTime().compareTo(seckill.getEndTime()) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动开始时间不能晚于结束时间");
        }
        seckill.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN)));
        int rows = seckillMapper.update(seckill);
        evictActiveCache();
        return rows;
    }

    @Override
    public int updateEnabled(Long id, boolean enabled) {
        if (id == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少活动 id");
        }
        int rows = seckillMapper.updateEnabled(id, enabled);
        evictActiveCache();
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long id) {
        if (id == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少活动 id");
        }
        assertNotStarted(id);
        // 级联删除活动商品，避免残留脏数据
        for (SeckillGoodEntity good : seckillMapper.findGoodsBySeckillId(id)) {
            redisTemplate.delete(stockKey(good.getId()));
        }
        seckillMapper.deleteGoodsBySeckillId(id);
        int rows = seckillMapper.deleteById(id);
        evictActiveCache();
        return rows;
    }

    @Override
    public List<SeckillEntity> listActive() {
        // 先读缓存，未命中回源数据库并回填；Redis 异常时直接回源，不影响可用性
        List<SeckillEntity> cached = readActiveCache();
        if (cached != null) {
            return cached;
        }
        List<SeckillEntity> active = seckillMapper.findActive();
        writeActiveCache(active);
        return active;
    }

    /** 读进行中活动缓存；未命中或 Redis 异常返回 null（由调用方回源数据库） */
    private List<SeckillEntity> readActiveCache() {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY_ACTIVE);
            if (json != null) {
                return JsonUtil.fromJson(json, new TypeReference<List<SeckillEntity>>() { });
            }
        } catch (Exception e) {
            log.warn("秒杀活动缓存读取失败，回源数据库: {}", e.getMessage());
        }
        return null;
    }

    /** 写进行中活动缓存（失败仅告警，不阻断查询） */
    private void writeActiveCache(List<SeckillEntity> active) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY_ACTIVE, JsonUtil.toJson(active), CACHE_TTL);
        } catch (Exception e) {
            log.warn("秒杀活动缓存写入失败: {}", e.getMessage());
        }
    }

    /** 活动变更后失效缓存，下次查询回源重建（失败仅告警，TTL 到期后自然收敛） */
    private void evictActiveCache() {
        try {
            redisTemplate.delete(CACHE_KEY_ACTIVE);
        } catch (Exception e) {
            log.warn("秒杀活动缓存失效失败: {}", e.getMessage());
        }
    }

    /* ==================== 秒杀活动商品管理 ==================== */

    @Override
    public List<SeckillGoodEntity> listGoods(Long seckillId) {
        if (seckillId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少活动 id");
        }
        List<SeckillGoodEntity> goods = seckillMapper.findGoodsBySeckillId(seckillId);
        // 经 Feign 批量回显商品信息；商品服务不可用时降级为 good=null，不影响列表主体
        // stock is the remaining inventory; sold is the completed/reserved count.
        fillGoods(goods);
        for (SeckillGoodEntity good : goods) {
            try {
                String remaining = redisTemplate.opsForValue().get(stockKey(good.getId()));
                if (remaining != null) good.setStock(Math.max(0, Integer.parseInt(remaining)));
            } catch (Exception e) {
                log.warn("秒杀库存读取失败 goodId={}: {}", good.getId(), e.getMessage());
            }
        }
        return goods;
    }

    /** 按 goodId 批量回显商品信息（Feign 调用失败/返回异常时跳过填充） */
    private void fillGoods(List<SeckillGoodEntity> goods) {
        if (goods == null || goods.isEmpty()) {
            return;
        }
        List<Long> goodIds = goods.stream()
                .map(SeckillGoodEntity::getGoodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (goodIds.isEmpty()) {
            return;
        }
        try {
            TypedJsonResp<List<GoodEntity>> resp = goodFeignClient.listByIds(goodIds);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                return;
            }
            Map<Long, GoodEntity> goodMap = resp.getData().stream()
                    .collect(Collectors.toMap(GoodEntity::getId, Function.identity(), (a, b) -> a));
            for (SeckillGoodEntity seckillGood : goods) {
                seckillGood.setGood(goodMap.get(seckillGood.getGoodId()));
            }
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败，跳过秒杀商品回显: {}", e.getMessage());
        }
    }

    @Override
    public SeckillGoodEntity getGoodById(Long seckillGoodId) {
        if (seckillGoodId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少秒杀商品 id");
        }
        SeckillGoodEntity good = seckillMapper.findGoodById(seckillGoodId);
        if (good != null) {
            try {
                String remaining = redisTemplate.opsForValue().get(stockKey(good.getId()));
                if (remaining != null) good.setStock(Math.max(0, Integer.parseInt(remaining)));
            } catch (Exception e) {
                log.warn("秒杀库存读取失败 goodId={}: {}", good.getId(), e.getMessage());
            }
        }
        return good;
    }

    @Override
    public int insertGood(SeckillGoodEntity seckillGood) {
        if (seckillGood == null || seckillGood.getSeckillId() == null || seckillGood.getGoodId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动与商品不能为空");
        }
        if (seckillGood.getSeckillPrice() == null || seckillGood.getSeckillPrice().signum() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "秒杀价不能为空且不能为负");
        }
        if (seckillGood.getStock() == null || seckillGood.getStock() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "秒杀库存不能为空且不能为负");
        }
        SeckillEntity seckill = seckillMapper.findById(seckillGood.getSeckillId());
        if (seckill == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "秒杀活动不存在");
        }
        if (seckillMapper.findGoodBySeckillAndGood(seckillGood.getSeckillId(), seckillGood.getGoodId()) != null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该商品已加入此活动，请勿重复添加");
        }
        if (seckillGood.getSold() == null) {
            seckillGood.setSold(0);
        }
        if (seckillGood.getLimitPerUser() == null) {
            seckillGood.setLimitPerUser(1);
        }
        int rows = seckillMapper.saveGood(seckillGood);
        if (rows > 0) {
            notifySeckillJoin(seckill, seckillGood);
        }
        return rows;
    }

    /**
     * 商品加入秒杀活动后通知收藏该商品的会员（收藏服务站内信）。
     * 通知属旁路逻辑：商品名回显/远程发送失败仅告警，不影响活动商品保存
     */
    private void notifySeckillJoin(SeckillEntity seckill, SeckillGoodEntity seckillGood) {
        try {
            SeckillJoinNoticeParam param = new SeckillJoinNoticeParam();
            param.setGoodId(seckillGood.getGoodId());
            param.setGoodName(resolveGoodName(seckillGood.getGoodId()));
            param.setSeckillName(seckill.getName());
            param.setSeckillPrice(seckillGood.getSeckillPrice());
            collectNoticeFeignClient.notifySeckillJoin(param);
        } catch (Exception e) {
            log.warn("进入秒杀通知发送失败 seckillId={} goodId={}: {}",
                    seckillGood.getSeckillId(), seckillGood.getGoodId(), e.getMessage());
        }
    }

    /** 经 good-api Feign 取商品名（专供通知文案；失败/缺失返回 null，文案回退为「商品」） */
    private String resolveGoodName(Long goodId) {
        try {
            TypedJsonResp<List<GoodEntity>> resp = goodFeignClient.listByIds(List.of(goodId));
            if (resp != null && Boolean.TRUE.equals(resp.getSuccess())
                    && resp.getData() != null && !resp.getData().isEmpty()) {
                return resp.getData().get(0).getName();
            }
        } catch (Exception e) {
            log.warn("商品名回显失败 goodId={}: {}", goodId, e.getMessage());
        }
        return null;
    }

    @Override
    public int updateGood(SeckillGoodEntity seckillGood) {
        if (seckillGood == null || seckillGood.getId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少秒杀商品 id");
        }
        SeckillGoodEntity existing = seckillMapper.findGoodById(seckillGood.getId());
        if (existing == null) throw new BizException(ResponseCode.NOT_FOUND, "秒杀商品不存在");
        assertNotStarted(existing.getSeckillId());
        if (seckillGood.getSeckillPrice() != null && seckillGood.getSeckillPrice().signum() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "秒杀价不能为负");
        }
        if (seckillGood.getStock() != null && seckillGood.getStock() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "秒杀库存不能为负");
        }
        if (seckillGood.getSold() != null && seckillGood.getSold() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "已售数量不能为负");
        }
        if (seckillGood.getLimitPerUser() != null && seckillGood.getLimitPerUser() < 1) {
            throw new BizException(ResponseCode.BAD_REQUEST, "每人限购数不能小于 1");
        }
        int rows = seckillMapper.updateGood(seckillGood);
        redisTemplate.delete(stockKey(seckillGood.getId()));
        return rows;
    }

    @Override
    public int deleteGood(Long seckillGoodId) {
        if (seckillGoodId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少秒杀商品 id");
        }
        SeckillGoodEntity existing = seckillMapper.findGoodById(seckillGoodId);
        if (existing == null) return 0;
        assertNotStarted(existing.getSeckillId());
        int rows = seckillMapper.deleteGoodById(seckillGoodId);
        redisTemplate.delete(stockKey(seckillGoodId));
        return rows;
    }

    private void assertNotStarted(Long seckillId) {
        SeckillEntity event = seckillMapper.findById(seckillId);
        if (event != null && event.getStartTime() != null
                && event.getStartTime().compareTo(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN))) <= 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "活动已开始，不能修改秒杀商品或删除活动");
        }
    }

    /* ==================== 库存预热与抢购 ==================== */

    private static String stockKey(Long goodId) { return "seckill:stock:" + goodId; }
    private static String userKey(Long memberId, Long goodId) {
        return "seckill:user:" + memberId + ":" + goodId;
    }

    private long ttlSeconds(SeckillEntity seckill) {
        LocalDateTime end = LocalDateTime.parse(seckill.getEndTime(),
                DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        return Math.max(3600, Duration.between(LocalDateTime.now(), end.plusHours(1)).getSeconds());
    }

    private void primeGood(SeckillGoodEntity good, SeckillEntity seckill) {
        redisTemplate.opsForValue().setIfAbsent(stockKey(good.getId()), String.valueOf(good.getStock()),
                Duration.ofSeconds(ttlSeconds(seckill)));
    }

    @Override
    public void warmUp(Long seckillId) {
        SeckillEntity seckill = seckillMapper.findById(seckillId);
        if (seckill == null) throw new BizException(ResponseCode.NOT_FOUND, "秒杀活动不存在");
        for (SeckillGoodEntity good : seckillMapper.findGoodsBySeckillId(seckillId)) {
            primeGood(good, seckill);
        }
    }

    @Override
    public String grab(Long seckillGoodId, Long memberId, String memberAccount, Long addrId) {
        if (seckillGoodId == null || memberId == null || isBlank(memberAccount) || addrId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "请选择收货地址并登录后抢购");
        }
        SeckillGoodEntity good = seckillMapper.findGoodById(seckillGoodId);
        if (good == null) throw new BizException(ResponseCode.NOT_FOUND, "秒杀商品不存在");
        SeckillEntity event = seckillMapper.findById(good.getSeckillId());
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        if (event == null || !Boolean.TRUE.equals(event.getEnabled())
                || now.compareTo(event.getStartTime()) < 0 || now.compareTo(event.getEndTime()) >= 0) {
            throw new BizException(ResponseCode.SECKILL_NOT_OPEN, "当前不在秒杀时间内");
        }
        int limit = good.getLimitPerUser() == null ? 1 : good.getLimitPerUser();
        long ttl = ttlSeconds(event);
        try {
            primeGood(good, event);
            Long claimed = redisTemplate.execute(CLAIM_SCRIPT,
                    List.of(stockKey(seckillGoodId), userKey(memberId, seckillGoodId)),
                    String.valueOf(limit), String.valueOf(ttl));
            if (claimed == null || claimed == -1) throw new BizException(ResponseCode.INTERNAL_ERROR, "秒杀库存未就绪");
            if (claimed == -2) throw new BizException(ResponseCode.SECKILL_SOLD_OUT, "秒杀商品已售罄");
            if (claimed == -3) throw new BizException(ResponseCode.SECKILL_ALREADY_JOINED, "已达到该商品限购数量");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("秒杀 Redis 扣减失败: {}", e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "秒杀服务暂不可用，请稍后重试");
        }
        String seckillNo = UUID.randomUUID().toString().replace("-", "");
        SeckillGrabEntity grab = new SeckillGrabEntity();
        grab.setSeckillNo(seckillNo);
        grab.setSeckillGoodId(seckillGoodId);
        grab.setMemberId(memberId);
        grab.setMemberAccount(memberAccount);
        grab.setAddrId(addrId);
        grab.setSeckillPrice(good.getSeckillPrice());
        grab.setStatus("PENDING");
        try {
            seckillMapper.insertGrab(grab);
            rabbitTemplate.convertAndSend(SeckillRabbitConfig.EXCHANGE,
                    SeckillRabbitConfig.ROUTING_KEY, seckillNo);
        } catch (Exception e) {
            seckillMapper.markFailed(seckillNo);
            restoreRedisClaim(grab);
            log.error("秒杀消息发送失败 seckillNo={}: {}", seckillNo, e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "抢购提交失败，请稍后重试");
        }
        return seckillNo;
    }

    @Override
    public GrabResult getGrabResult(String orderNo, Long memberId) {
        SeckillGrabEntity grab = seckillMapper.findGrab(orderNo);
        if (grab == null) throw new BizException(ResponseCode.NOT_FOUND, "抢购记录不存在");
        if (memberId == null || !memberId.equals(grab.getMemberId())) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权查看他人抢购结果");
        }
        GrabResult result = new GrabResult();
        result.setOrderNo(orderNo);
        result.setActualOrderNo(grab.getOrderNo());
        result.setStatus(switch (grab.getStatus()) {
            case "SUCCESS" -> "成功";
            case "FAILED", "CANCELLED" -> "失败";
            default -> "处理中";
        });
        return result;
    }

    public void processGrab(String seckillNo) {
        SeckillReservationService.ReserveResult reserved = reservationService.reserve(seckillNo);
        SeckillGrabEntity grab = seckillMapper.findGrab(seckillNo);
        if (reserved == SeckillReservationService.ReserveResult.FINISHED) return;
        if (reserved == SeckillReservationService.ReserveResult.REJECTED) {
            restoreRedisClaim(grab);
            return;
        }
        SeckillGoodEntity good = seckillMapper.findGoodById(grab.getSeckillGoodId());
        SeckillOrderParam param = new SeckillOrderParam();
        param.setSeckillNo(seckillNo);
        param.setSeckillGoodId(grab.getSeckillGoodId());
        param.setGoodId(good.getGoodId());
        param.setMemberId(grab.getMemberId());
        param.setMemberAccount(grab.getMemberAccount());
        param.setAddrId(grab.getAddrId());
        param.setSeckillPrice(grab.getSeckillPrice());
        TypedJsonResp<OrderEntity> response = orderFeignClient.createSeckillOrder(param);
        if (response == null) throw new IllegalStateException("订单服务未返回响应");
        if (!Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {
            if (reservationService.failProcessing(seckillNo)) restoreRedisClaim(grab);
            return;
        }
        seckillMapper.markSuccess(seckillNo, response.getData().getOrderNo());
    }

    public int releaseGrab(String seckillNo) {
        SeckillGrabEntity grab = seckillMapper.findGrab(seckillNo);
        if (grab == null || !reservationService.release(seckillNo)) return 0;
        restoreRedisClaim(grab);
        return 1;
    }

    /** Repairs a missed cross-service release after an order was cancelled. */
    @Scheduled(fixedDelay = 60_000)
    public void reconcileCancelledOrders() {
        for (String seckillNo : seckillMapper.findCancelledSuccessGrabs()) {
            try {
                releaseGrab(seckillNo);
            } catch (Exception e) {
                log.error("秒杀取消订单回补重试失败 seckillNo={}: {}", seckillNo, e.getMessage());
            }
        }
    }

    /** Republishes accepted grabs left waiting after a producer or consumer interruption. */
    @Scheduled(fixedDelay = 30_000)
    public void retryUnfinishedGrabs() {
        for (String seckillNo : seckillMapper.findRetryableGrabs()) {
            try {
                if (seckillMapper.touchRetry(seckillNo) > 0) {
                    rabbitTemplate.convertAndSend(SeckillRabbitConfig.EXCHANGE,
                            SeckillRabbitConfig.ROUTING_KEY, seckillNo);
                }
            } catch (Exception e) {
                log.error("秒杀消息重投失败 seckillNo={}: {}", seckillNo, e.getMessage());
            }
        }
    }

    private void restoreRedisClaim(SeckillGrabEntity grab) {
        if (grab == null) return;
        try {
            redisTemplate.execute(RELEASE_SCRIPT,
                    List.of(stockKey(grab.getSeckillGoodId()), userKey(grab.getMemberId(), grab.getSeckillGoodId())));
        } catch (Exception e) {
            log.error("Redis 秒杀名额回补失败 seckillNo={}: {}", grab.getSeckillNo(), e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
