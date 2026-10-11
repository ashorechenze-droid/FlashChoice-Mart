package com.example.fcmordermicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmcommon.util.SnowflakeIdGenerator;
import com.example.fcmcommon.util.XssUtil;
import com.example.fcmordermicroservice.config.RabbitMqConfig;
import com.example.fcmordermicroservice.feign.CartFeignClient;
import com.example.fcmordermicroservice.feign.GoodFeignClient;
import com.example.fcmordermicroservice.feign.MemberAddressFeignClient;
import com.example.fcmordermicroservice.feign.SeckillFeignClient;
import com.example.fcmordermicroservice.mapper.OrderMapper;
import com.example.fcmspi.model.CartEntity;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.MemberAddressEntity;
import com.example.fcmspi.model.OrderCreateParam;
import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.OrderItemEntity;
import com.example.fcmspi.model.SeckillOrderParam;
import com.example.fcmspi.model.StockChangeParam;
import com.example.fcmspi.model.search.OrderSearchBean;
import com.example.fcmspi.service.OrderService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /* ===== 订单状态（与表字典一致） ===== */
    private static final String STATUS_UNPAID = "待付款";
    private static final String STATUS_PAID = "已支付";
    private static final String STATUS_UNRECEIVED = "待收货";
    private static final String STATUS_CONFIRMED = "已确认";
    private static final String STATUS_CANCELLED = "已取消";
    /* ===== 退款状态 ===== */
    private static final String REFUND_NONE = "无退款";
    private static final String REFUND_ING = "退款中";
    private static final String REFUND_DONE = "已退款";

    /** 超时关单时间窗（30 分钟） */
    private static final long TIMEOUT_MINUTES = 30L;

    private final OrderMapper orderMapper;
    private final GoodFeignClient goodFeignClient;
    private final CartFeignClient cartFeignClient;
    private final MemberAddressFeignClient memberAddressFeignClient;
    private final SeckillFeignClient seckillFeignClient;
    private final RabbitTemplate rabbitTemplate;
    /** 订单号雪花 ID 生成器（本地单实例 workerId/dataCenterId 固定 1，多实例部署需保证不重复） */
    private final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

    public OrderServiceImpl(OrderMapper orderMapper,
                            GoodFeignClient goodFeignClient,
                            CartFeignClient cartFeignClient,
                            MemberAddressFeignClient memberAddressFeignClient,
                            SeckillFeignClient seckillFeignClient,
                            RabbitTemplate rabbitTemplate) {
        this.orderMapper = orderMapper;
        this.goodFeignClient = goodFeignClient;
        this.cartFeignClient = cartFeignClient;
        this.memberAddressFeignClient = memberAddressFeignClient;
        this.seckillFeignClient = seckillFeignClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
    }

    /** 订单号生成：雪花 ID（全局趋势递增，便于索引与后续分库分表） */
    private String generateOrderNo() {
        return String.valueOf(idGenerator.nextId());
    }

    @Override
    public PageInfo<OrderEntity> page(OrderSearchBean search, PaginateInfo page) {
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        PageInfo<OrderEntity> pageInfo = new PageInfo<>(orderMapper.findAll(search));
        attachItems(pageInfo.getList());
        return pageInfo;
    }

    /** 列表批量装配明细（一次 IN 查询）：管理端订单列表展示与分类销售统计依赖 orderItems */
    private void attachItems(List<OrderEntity> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(OrderEntity::getId).toList();
        Map<Long, List<OrderItemEntity>> itemsByOrder = orderMapper.findItemsByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        orders.forEach(order -> order.setOrderItems(itemsByOrder.getOrDefault(order.getId(), List.of())));
    }

    @Override
    public OrderEntity getById(Long id, boolean withItems) {
        OrderEntity order = orderMapper.findById(id);
        if (order != null && withItems) {
            order.setOrderItems(orderMapper.findItemsByOrderId(id));
        }
        return order;
    }

    @Override
    public OrderEntity getByOrderNo(String orderNo, boolean withItems) {
        OrderEntity order = orderMapper.findByOrderNo(orderNo);
        if (order != null && withItems) {
            order.setOrderItems(orderMapper.findItemsByOrderId(order.getId()));
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createOrder(OrderCreateParam param) {
        if (param.getMemberAccount() == null || param.getMemberAccount().isBlank()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少下单会员账号");
        }
        boolean fromCart = param.getCartIds() != null && !param.getCartIds().isEmpty();
        if (!fromCart && param.getGoodId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "请选择购物车条目或商品后下单");
        }
        if (!fromCart && (param.getQty() == null || param.getQty() < 1)) {
            throw new BizException(ResponseCode.BAD_REQUEST, "购买数量需大于 0");
        }
        if (param.getAddrId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "请选择收货地址");
        }
        String now = now();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setMemberAccount(param.getMemberAccount());
        order.setStatus(STATUS_UNPAID);
        order.setRefundStatus(REFUND_NONE);
        order.setCheckoutTime(now);
        // 留言为纯文本字段，剥离 HTML 标签防 XSS（详细设计 8.2 输入安全）
        order.setOrderComment(XssUtil.stripTags(param.getComment()));
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setIsDel(false);
        // 1. 收货地址快照：member-address-api 按 addrId 取地址（缺失/非本人直接拒绝下单）
        fillAddressSnapshot(order, param);
        // 2. 明细组装与快照：cart-api 取勾选条目（或立即购买单条），good-api 校验在售并快照名称/价格
        List<OrderItemEntity> items = buildItems(param, fromCart);
        order.setTotalPay(aggregateTotalPay(items));
        // 3. 库存扣减：good-api 条件更新（qty >= count），任一不足整体失败回滚
        List<StockChangeParam> stockChanges = toStockChanges(items);
        deductStock(stockChanges);
        // 4. 订单与明细落库；失败时补偿回补已扣库存（跨服务无全局事务，尽力补偿）
        try {
            orderMapper.save(order);
            items.forEach(item -> item.setOrderId(order.getId()));
            orderMapper.saveItems(items);
        } catch (Exception e) {
            log.error("订单落库失败，补偿回补库存 orderNo={}: {}", order.getOrderNo(), e.getMessage());
            restoreStock(stockChanges);
            throw e;
        }
        // 5. 购物车勾选清除（失败降级：订单已生成，条目残留可手动删除）
        if (fromCart) {
            clearCart(param.getMemberId(), param.getCartIds());
        }
        // 6. 发送超时关单延时消息（30 分钟未支付自动关闭；发送失败由定时扫描兜底）
        sendDelayCloseMessage(order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createSeckillOrder(SeckillOrderParam param) {
        if (param == null || param.getSeckillNo() == null || param.getSeckillNo().isBlank()
                || param.getGoodId() == null || param.getAddrId() == null
                || param.getMemberAccount() == null || param.getMemberAccount().isBlank()
                || param.getSeckillPrice() == null || param.getSeckillPrice().signum() < 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "秒杀订单参数不完整");
        }
        OrderEntity existing = orderMapper.findBySeckillNo(param.getSeckillNo());
        if (existing != null) return existing;

        OrderCreateParam basic = new OrderCreateParam();
        basic.setMemberAccount(param.getMemberAccount());
        basic.setMemberId(param.getMemberId());
        basic.setAddrId(param.getAddrId());
        basic.setGoodId(param.getGoodId());
        basic.setQty(1);
        String current = now();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setSeckillNo(param.getSeckillNo());
        order.setMemberAccount(param.getMemberAccount());
        order.setStatus(STATUS_UNPAID);
        order.setRefundStatus(REFUND_NONE);
        order.setCheckoutTime(current);
        order.setCreatedTime(current);
        order.setUpdatedTime(current);
        order.setIsDel(false);
        fillAddressSnapshot(order, basic);
        List<OrderItemEntity> items = buildItems(basic, false);
        items.getFirst().setDealPrice(param.getSeckillPrice());
        order.setTotalPay(param.getSeckillPrice());
        List<StockChangeParam> changes = toStockChanges(items);
        deductStock(changes);
        try {
            orderMapper.save(order);
            items.forEach(item -> item.setOrderId(order.getId()));
            orderMapper.saveItems(items);
        } catch (Exception e) {
            restoreStock(changes);
            throw e;
        }
        sendDelayCloseMessage(order.getOrderNo());
        return order;
    }

    /** 发送超时关单延时消息（TTL + 死信队列实现 30 分钟延时投递）；MQ 不可用仅告警，异步发送不增加下单耗时 */
    private void sendDelayCloseMessage(String orderNo) {
        CompletableFuture.runAsync(() -> {
            try {
                rabbitTemplate.convertAndSend(RabbitMqConfig.DELAY_EXCHANGE,
                        RabbitMqConfig.DELAY_ROUTING_KEY, orderNo);
                log.info("已发送超时关单延时消息 orderNo={}", orderNo);
            } catch (Exception e) {
                log.error("超时关单延时消息发送失败（定时扫描兜底）orderNo={}: {}", orderNo, e.getMessage());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int payConfirm(Long id, String memberAccount, String payType) {
        OrderEntity order = requireOwnOrder(id, memberAccount);
        if (!STATUS_UNPAID.equals(order.getStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "订单当前状态不允许支付");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setStatus(STATUS_PAID);
        upd.setPayType(payType);
        upd.setAlipayTradeNo("MOCK" + System.currentTimeMillis());
        upd.setPayTime(now());
        upd.setUpdatedTime(now());
        int rows = orderMapper.markPaid(upd);
        if (rows == 0) throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "订单状态已变更，请刷新后重试");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancel(Long id, String memberAccount) {
        OrderEntity order = requireOwnOrder(id, memberAccount);
        if (!STATUS_UNPAID.equals(order.getStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "仅待付款订单可取消");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setStatus(STATUS_CANCELLED);
        upd.setUpdatedTime(now());
        int rows = orderMapper.markCancelled(id, upd.getUpdatedTime());
        if (rows == 0) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "订单状态已变更，请刷新后重试");
        }
        // 按明细经 good-api 回补库存（失败仅告警，不影响取消结果，可人工兜底）
        restoreStock(toStockChanges(orderMapper.findItemsByOrderId(id)));
        releaseSeckill(order);
        return rows;
    }

    @Override
    public int ship(Long id) {
        OrderEntity order = requireOwnOrder(id, null);
        if (!STATUS_PAID.equals(order.getStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "仅已支付订单可发货");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setStatus(STATUS_UNRECEIVED);
        upd.setShipTime(now());
        upd.setUpdatedTime(now());
        return orderMapper.updateStatus(upd);
    }

    @Override
    public int confirmReceive(Long id, String memberAccount) {
        OrderEntity order = requireOwnOrder(id, memberAccount);
        if (!STATUS_UNRECEIVED.equals(order.getStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "仅待收货订单可确认收货");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setStatus(STATUS_CONFIRMED);
        upd.setAcceptTime(now());
        upd.setUpdatedTime(now());
        return orderMapper.updateStatus(upd);
    }

    @Override
    public int applyRefund(Long id, String refundReason, String memberAccount) {
        OrderEntity order = requireOwnOrder(id, memberAccount);
        if (!REFUND_NONE.equals(order.getRefundStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "仅无退款记录的订单可发起退款");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setRefundStatus(REFUND_ING);
        if (refundReason != null && !refundReason.isBlank()) {
            // 退款原因展示在订单详情，同样做 XSS 过滤
            upd.setDescription(XssUtil.stripTags(refundReason));
        }
        upd.setUpdatedTime(now());
        return orderMapper.updateStatus(upd);
    }

    @Override
    public int confirmRefund(Long id) {
        OrderEntity order = requireOwnOrder(id, null);
        if (!REFUND_ING.equals(order.getRefundStatus())) {
            throw new BizException(ResponseCode.ORDER_STATUS_INVALID, "仅退款中的订单可确认退款");
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(id);
        upd.setRefundStatus(REFUND_DONE);
        upd.setUpdatedTime(now());
        return orderMapper.updateStatus(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutOrder(String orderNo) {
        OrderEntity order = orderMapper.findByOrderNo(orderNo);
        if (order == null || !STATUS_UNPAID.equals(order.getStatus())) {
            return 0;
        }
        OrderEntity upd = new OrderEntity();
        upd.setId(order.getId());
        upd.setStatus(STATUS_CANCELLED);
        upd.setUpdatedTime(now());
        int rows = orderMapper.markCancelled(order.getId(), upd.getUpdatedTime());
        if (rows == 0) {
            return 0;
        }
        // 按明细经 good-api 回补库存（失败仅告警，可人工兜底）
        restoreStock(toStockChanges(orderMapper.findItemsByOrderId(order.getId())));
        releaseSeckill(order);
        return rows;
    }

    private void releaseSeckill(OrderEntity order) {
        if (order.getSeckillNo() == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { doReleaseSeckill(order.getSeckillNo()); }
            });
        } else {
            doReleaseSeckill(order.getSeckillNo());
        }
    }

    private void doReleaseSeckill(String seckillNo) {
        try {
            TypedJsonResp<Integer> response = seckillFeignClient.release(seckillNo);
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                log.error("秒杀库存回补失败 seckillNo={}: {}", seckillNo,
                        response == null ? "empty response" : response.getMsg());
            }
        } catch (Exception e) {
            log.error("秒杀库存回补失败 seckillNo={}: {}", seckillNo, e.getMessage());
        }
    }

    /** 定时扫描兜底：每分钟关闭超时未支付订单并逐单回补库存（RabbitMQ 延时消息为主链路，扫描为最终兜底） */
    @Override
    @Scheduled(fixedDelay = 60_000)
    public void closeTimeoutOrders() {
        List<String> orderNos = orderMapper.findTimeoutOrderNos();
        for (String orderNo : orderNos) {
            try {
                closeTimeoutOrder(orderNo);
            } catch (Exception e) {
                log.error("超时关单失败 orderNo={}: {}", orderNo, e.getMessage());
            }
        }
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return orderMapper.deleteByIds(ids);
    }

    /* ==================== 跨服务装配（Feign） ==================== */

    /** 收货地址快照：经 member-address-api 取地址并校验归属，缺失/远程失败直接拒绝下单 */
    private void fillAddressSnapshot(OrderEntity order, OrderCreateParam param) {
        MemberAddressEntity addr;
        try {
            TypedJsonResp<MemberAddressEntity> resp = memberAddressFeignClient.getById(param.getAddrId());
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                throw new BizException(ResponseCode.NOT_FOUND, "收货地址不存在");
            }
            addr = resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("地址服务 Feign 调用失败 addrId={}: {}", param.getAddrId(), e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "地址服务暂不可用，请稍后重试");
        }
        if (!param.getMemberAccount().equals(addr.getMemberAccount())) {
            throw new BizException(ResponseCode.FORBIDDEN, "收货地址不属于当前会员");
        }
        order.setReceiverAddrId(addr.getId());
        order.setReceiverName(addr.getReceiver());
        order.setReceiverPhone(addr.getPhone());
        order.setReceiverAddrDetail(addr.getAddrDetail());
    }

    /** 明细组装与快照：购物车勾选（cart-api）或立即购买单条；商品经 good-api 校验在售与库存后快照 */
    private List<OrderItemEntity> buildItems(OrderCreateParam param, boolean fromCart) {
        Map<Long, Integer> qtyByGood = new LinkedHashMap<>();
        if (fromCart) {
            if (param.getMemberId() == null) {
                throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员身份（X-User-Id），无法结算购物车");
            }
            Map<Long, CartEntity> cartMap = listCarts(param.getCartIds()).stream()
                    .collect(Collectors.toMap(CartEntity::getId, Function.identity(), (a, b) -> a));
            // 去重遍历：重复 cartId 不能造成同一商品数量翻倍
            for (Long cartId : new LinkedHashSet<>(param.getCartIds())) {
                CartEntity cart = cartMap.get(cartId);
                if (cart == null) {
                    throw new BizException(ResponseCode.NOT_FOUND, "购物车条目不存在: " + cartId);
                }
                if (!param.getMemberId().equals(cart.getMemberId())) {
                    throw new BizException(ResponseCode.FORBIDDEN, "无权结算他人购物车条目");
                }
                qtyByGood.merge(cart.getGoodId(), cart.getQty(), Integer::sum);
            }
        } else {
            qtyByGood.put(param.getGoodId(), param.getQty());
        }
        Map<Long, GoodEntity> goodMap = listGoods(qtyByGood.keySet()).stream()
                .collect(Collectors.toMap(GoodEntity::getId, Function.identity(), (a, b) -> a));
        List<OrderItemEntity> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : qtyByGood.entrySet()) {
            GoodEntity good = goodMap.get(entry.getKey());
            if (good == null) {
                throw new BizException(ResponseCode.NOT_FOUND, "商品不存在: " + entry.getKey());
            }
            if (Boolean.TRUE.equals(good.getIsDel()) || Boolean.TRUE.equals(good.getIsTakeDown())) {
                throw new BizException(ResponseCode.BAD_REQUEST, "商品已下架: " + good.getName());
            }
            if (good.getQty() == null || good.getQty() < entry.getValue()) {
                throw new BizException(ResponseCode.STOCK_NOT_ENOUGH, "库存不足: " + good.getName());
            }
            OrderItemEntity item = new OrderItemEntity();
            item.setGoodId(good.getId());
            item.setDealPrice(good.getPrice());
            item.setCount(entry.getValue());
            item.setGoodName(good.getName());
            item.setGoodPic(good.getPic());
            item.setGoodDesc(good.getSummary());
            items.add(item);
        }
        return items;
    }

    /** 按 cartIds 批量查询购物车条目（cart-api；远程失败直接拒绝下单） */
    private List<CartEntity> listCarts(List<Long> cartIds) {
        try {
            TypedJsonResp<List<CartEntity>> resp = cartFeignClient.listByIds(cartIds);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                throw new BizException(ResponseCode.INTERNAL_ERROR, "购物车服务返回异常，请稍后重试");
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("购物车服务 Feign 调用失败: {}", e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "购物车服务暂不可用，请稍后重试");
        }
    }

    /** 按 goodIds 批量查询商品（good-api；远程失败直接拒绝下单） */
    private List<GoodEntity> listGoods(Collection<Long> goodIds) {
        try {
            TypedJsonResp<List<GoodEntity>> resp = goodFeignClient.listByIds(List.copyOf(goodIds));
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                throw new BizException(ResponseCode.INTERNAL_ERROR, "商品服务返回异常，请稍后重试");
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("商品服务 Feign 调用失败: {}", e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "商品服务暂不可用，请稍后重试");
        }
    }

    /** 订单应付总额 = Σ(成交价 × 数量)，成交价缺失按 0 计 */
    private BigDecimal aggregateTotalPay(List<OrderItemEntity> items) {
        return items.stream()
                .map(item -> (item.getDealPrice() == null ? BigDecimal.ZERO : item.getDealPrice())
                        .multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 明细转库存变更参数（下单扣减与取消/超时回补共用） */
    private List<StockChangeParam> toStockChanges(List<OrderItemEntity> items) {
        List<StockChangeParam> changes = new ArrayList<>();
        if (items == null) {
            return changes;
        }
        for (OrderItemEntity item : items) {
            StockChangeParam change = new StockChangeParam();
            change.setGoodId(item.getGoodId());
            change.setCount(item.getCount());
            changes.add(change);
        }
        return changes;
    }

    /** 库存扣减（good-api 条件更新；库存不足/已下架与远程失败均拒绝下单） */
    private void deductStock(List<StockChangeParam> changes) {
        TypedJsonResp<Integer> resp;
        try {
            resp = goodFeignClient.deductStock(changes);
        } catch (Exception e) {
            log.error("商品服务库存扣减调用失败: {}", e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "商品服务暂不可用，请稍后重试");
        }
        if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
            int code = resp != null && resp.getCode() != null ? resp.getCode() : ResponseCode.INTERNAL_ERROR;
            String msg = resp != null && resp.getMsg() != null ? resp.getMsg() : "库存扣减失败";
            throw new BizException(code, msg);
        }
    }

    /** 库存回补（good-api；失败仅告警不阻断，可人工兜底） */
    private void restoreStock(List<StockChangeParam> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        try {
            TypedJsonResp<Integer> resp = goodFeignClient.restoreStock(changes);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                log.warn("库存回补未成功: {}", resp != null ? resp.getMsg() : "响应为空");
            }
        } catch (Exception e) {
            log.error("商品服务库存回补调用失败: {}", e.getMessage());
        }
    }

    /** 清除勾选的购物车条目（cart-api；失败仅告警，订单已生成不受影响） */
    private void clearCart(Long memberId, List<Long> cartIds) {
        try {
            TypedJsonResp<Integer> resp = cartFeignClient.deleteByIds(memberId, cartIds);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                log.warn("购物车清除未成功: {}", resp != null ? resp.getMsg() : "响应为空");
            }
        } catch (Exception e) {
            log.error("购物车服务 Feign 删除调用失败: {}", e.getMessage());
        }
    }

    /** 查询订单并做归属校验（memberAccount 为空表示后台操作，跳过归属校验） */
    private OrderEntity requireOwnOrder(Long id, String memberAccount) {
        OrderEntity order = orderMapper.findById(id);
        if (order == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "订单不存在");
        }
        if (memberAccount != null && !memberAccount.isBlank() && !order.getMemberAccount().equals(memberAccount)) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权操作他人订单");
        }
        return order;
    }
}
