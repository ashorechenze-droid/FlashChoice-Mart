package com.example.fcmgoodmicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmcommon.util.JsonUtil;
import com.example.fcmcommon.util.XssUtil;
import com.example.fcmgoodmicroservice.feign.BrandFeignClient;
import com.example.fcmgoodmicroservice.feign.CategoryFeignClient;
import com.example.fcmgoodmicroservice.feign.CollectNoticeFeignClient;
import com.example.fcmgoodmicroservice.mapper.GoodMapper;
import com.example.fcmspi.model.BrandEntity;
import com.example.fcmspi.model.CategoryEntity;
import com.example.fcmspi.model.GoodDetailPicEntity;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.PriceDropNoticeParam;
import com.example.fcmspi.model.StockChangeParam;
import com.example.fcmspi.model.search.GoodSearchBean;
import com.example.fcmspi.service.GoodService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GoodServiceImpl implements GoodService {

    /** 热销首屏缓存 key 前缀（拼 pageSize）；仅缓存"热销 + 第一页 + 无其他筛选"这一种查询 */
    private static final String CACHE_KEY_HOT_PREFIX = "fcm:cache:good:hot:";
    /** 热销缓存有效期（库存等实时变化字段允许 TTL 内延迟展示，详情/下单有实时校验兜底） */
    private static final Duration HOT_CACHE_TTL = Duration.ofMinutes(5);

    private final GoodMapper goodMapper;
    private final BrandFeignClient brandFeignClient;
    private final CategoryFeignClient categoryFeignClient;
    private final StringRedisTemplate redisTemplate;
    private final CollectNoticeFeignClient collectNoticeFeignClient;

    public GoodServiceImpl(GoodMapper goodMapper,
                           BrandFeignClient brandFeignClient,
                           CategoryFeignClient categoryFeignClient,
                           StringRedisTemplate redisTemplate,
                           CollectNoticeFeignClient collectNoticeFeignClient) {
        this.goodMapper = goodMapper;
        this.brandFeignClient = brandFeignClient;
        this.categoryFeignClient = categoryFeignClient;
        this.redisTemplate = redisTemplate;
        this.collectNoticeFeignClient = collectNoticeFeignClient;
    }

    @Override
    public PageInfo<GoodEntity> page(GoodSearchBean search, PaginateInfo page) {
        // 热销首屏（首页热销榜，访问最密集）走短 TTL 缓存
        if (cacheableHotQuery(search, page)) {
            return pageHotWithCache(search, page);
        }
        // 分类查询：传入分类ID时，递归收集该分类及其所有子分类ID，用 IN 查询
        // （对应详细设计书 category.parent_id 自关联树；Feign 调用失败时降级为精确匹配）
        if (search.getCategoryId() != null) {
            List<Long> ids = collectCategoryIdsRecursive(search.getCategoryId());
            if (ids != null && !ids.isEmpty()) {
                search.setCategoryIdList(ids);
            }
        }
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        return new PageInfo<>(goodMapper.findAll(search));
    }

    /**
     * 递归收集指定分类及其所有子分类的 ID。
     * 通过 Feign 调用分类服务 tree 接口获取整棵树，再在树中定位目标节点并递归收集。
     * Feign 调用失败或树中找不到目标分类时返回 null，上层回退为 categoryId 精确匹配。
     */
    private List<Long> collectCategoryIdsRecursive(Long rootCategoryId) {
        try {
            TypedJsonResp<List<CategoryEntity>> resp = categoryFeignClient.tree();
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                return null;
            }
            CategoryEntity target = findInTree(resp.getData(), rootCategoryId);
            if (target == null) {
                return null;
            }
            List<Long> ids = new ArrayList<>();
            collectIds(target, ids);
            return ids;
        } catch (Exception e) {
            log.warn("分类服务 tree 接口调用失败，降级为 categoryId 精确匹配 categoryId={}: {}",
                    rootCategoryId, e.getMessage());
            return null;
        }
    }

    /** 在分类树中查找指定 ID 的节点 */
    private CategoryEntity findInTree(List<CategoryEntity> nodes, Long id) {
        if (nodes == null) {
            return null;
        }
        for (CategoryEntity node : nodes) {
            if (node.getId() != null && node.getId().equals(id)) {
                return node;
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                CategoryEntity found = findInTree(node.getChildren(), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 递归收集节点及其所有子节点的 ID */
    private void collectIds(CategoryEntity node, List<Long> ids) {
        if (node == null || node.getId() == null) {
            return;
        }
        ids.add(node.getId());
        if (node.getChildren() != null) {
            for (CategoryEntity child : node.getChildren()) {
                collectIds(child, ids);
            }
        }
    }

    /* ==================== 热销首屏缓存 ==================== */

    /**
     * 是否命中热销首屏缓存场景：热销 + 第一页 + 无其他筛选条件。
     * 排序允许缺省或 hot：is_hot=1 过滤下两者均等价于 id desc（见 mapper.xml），可共用缓存
     */
    private boolean cacheableHotQuery(GoodSearchBean search, PaginateInfo page) {
        return Boolean.TRUE.equals(search.getIsHot())
                && page.getPageNo() != null && page.getPageNo() == 1
                && (search.getSort() == null || search.getSort().isBlank() || "hot".equals(search.getSort()))
                && (search.getKeyword() == null || search.getKeyword().isBlank())
                && search.getCategoryId() == null
                && search.getBrandId() == null
                && search.getPriceLow() == null
                && search.getPriceHigh() == null;
    }

    /** 热销首页分页：先读缓存（列表 + 总数），未命中回源并回填；Redis 异常时直接回源 */
    private PageInfo<GoodEntity> pageHotWithCache(GoodSearchBean search, PaginateInfo page) {
        String cacheKey = CACHE_KEY_HOT_PREFIX + page.getPageSize();
        HotPageCache cached = readHotCache(cacheKey);
        if (cached != null) {
            // 从缓存列表重建 PageInfo（total 用于前端分页器，pages 等派生字段自动重算）
            Page<GoodEntity> pageData = new Page<>(page.getPageNo(), page.getPageSize());
            pageData.setTotal(cached.getTotal());
            pageData.addAll(cached.getList());
            return new PageInfo<>(pageData);
        }
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        PageInfo<GoodEntity> pageInfo = new PageInfo<>(goodMapper.findAll(search));
        writeHotCache(cacheKey, new HotPageCache(pageInfo.getTotal(), pageInfo.getList()));
        return pageInfo;
    }

    /** 读热销缓存；未命中或 Redis 异常返回 null（由调用方回源数据库） */
    private HotPageCache readHotCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return JsonUtil.fromJson(json, HotPageCache.class);
            }
        } catch (Exception e) {
            log.warn("热销缓存读取失败，回源数据库: {}", e.getMessage());
        }
        return null;
    }

    /** 写热销缓存（失败仅告警，不阻断查询） */
    private void writeHotCache(String cacheKey, HotPageCache cache) {
        try {
            redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(cache), HOT_CACHE_TTL);
        } catch (Exception e) {
            log.warn("热销缓存写入失败: {}", e.getMessage());
        }
    }

    /** 商品变更后失效热销缓存（按前缀删除各 pageSize 变体；key 数量少，keys 命令可接受） */
    private void evictHotCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_HOT_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("热销缓存失效失败: {}", e.getMessage());
        }
    }

    /** 热销首屏分页缓存载体（PageInfo 派生字段多且不便序列化，仅缓存列表与总数） */
    @Data
    private static class HotPageCache {
        /** 满足条件的记录总数 */
        private long total;
        /** 当前页列表 */
        private List<GoodEntity> list;

        HotPageCache(long total, List<GoodEntity> list) {
            this.total = total;
            this.list = list;
        }
    }

    @Override
    public GoodEntity getById(Long id) {
        return goodMapper.findById(id);
    }

    @Override
    public GoodEntity getDetail(Long id) {
        GoodEntity good = goodMapper.findById(id);
        if (good != null) {
            // 品牌/分类经 Feign 组装；远程异常仅降级为空，不影响详情主体返回
            fillBrand(good);
            fillCategory(good);
            good.setDetailPicList(goodMapper.findDetailPics(id));
        }
        return good;
    }

    /** Feign 组装品牌（调用失败/无品牌 ID 时跳过，不阻断详情） */
    private void fillBrand(GoodEntity good) {
        if (good.getBrandId() == null) {
            return;
        }
        try {
            TypedJsonResp<BrandEntity> resp = brandFeignClient.getById(good.getBrandId());
            if (resp != null && Boolean.TRUE.equals(resp.getSuccess())) {
                good.setBrand(resp.getData());
            }
        } catch (Exception e) {
            log.warn("品牌服务 Feign 调用失败 brandId={}: {}", good.getBrandId(), e.getMessage());
        }
    }

    /** Feign 组装分类（调用失败/无分类 ID 时跳过，不阻断详情） */
    private void fillCategory(GoodEntity good) {
        if (good.getCategoryId() == null) {
            return;
        }
        try {
            TypedJsonResp<CategoryEntity> resp = categoryFeignClient.getById(good.getCategoryId());
            if (resp != null && Boolean.TRUE.equals(resp.getSuccess())) {
                good.setCategory(resp.getData());
            }
        } catch (Exception e) {
            log.warn("分类服务 Feign 调用失败 categoryId={}: {}", good.getCategoryId(), e.getMessage());
        }
    }

    @Override
    public List<GoodEntity> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return goodMapper.findByIds(ids);
    }

    @Override
    public int insert(GoodEntity good) {
        sanitizeTextFields(good);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        good.setCreatedTime(now);
        good.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        if (good.getIsTakeDown() == null) {
            good.setIsTakeDown(false);
        }
        if (good.getIsDel() == null) {
            good.setIsDel(false);
        }
        int rows = goodMapper.save(good);
        // 详情图随主表覆盖式保存（同库事务内由 service 调用方控制）
        saveDetailPics(good);
        evictHotCache();
        return rows;
    }

    @Override
    public int update(GoodEntity good) {
        sanitizeTextFields(good);
        good.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN)));
        // 改价前取旧记录：用于对比检测销售价下调（仅真实降价才通知收藏者）
        GoodEntity old = good.getId() == null ? null : goodMapper.findById(good.getId());
        int rows = goodMapper.update(good);
        evictHotCache();
        if (rows > 0) {
            notifyPriceDrop(old, good);
        }
        return rows;
    }

    /**
     * 销售价下调时通知收藏该商品的会员（收藏服务站内信）。
     * 通知属旁路逻辑：远程失败仅告警，不影响改价主流程
     */
    private void notifyPriceDrop(GoodEntity old, GoodEntity updated) {
        if (old == null || updated.getPrice() == null || old.getPrice() == null) {
            return;
        }
        if (old.getPrice().compareTo(updated.getPrice()) <= 0) {
            return;
        }
        // 已下架/逻辑删除的商品不打扰用户（未传值时沿用旧值判断）
        Boolean takeDown = updated.getIsTakeDown() != null ? updated.getIsTakeDown() : old.getIsTakeDown();
        Boolean isDel = updated.getIsDel() != null ? updated.getIsDel() : old.getIsDel();
        if (Boolean.TRUE.equals(takeDown) || Boolean.TRUE.equals(isDel)) {
            return;
        }
        try {
            PriceDropNoticeParam param = new PriceDropNoticeParam();
            param.setGoodId(updated.getId());
            param.setGoodName(updated.getName() != null ? updated.getName() : old.getName());
            param.setOldPrice(old.getPrice());
            param.setNewPrice(updated.getPrice());
            collectNoticeFeignClient.notifyPriceDrop(param);
        } catch (Exception e) {
            log.warn("降价通知发送失败 goodId={}: {}", updated.getId(), e.getMessage());
        }
    }

    @Override
    public int updateTakeDown(List<Long> ids, Boolean isTakeDown) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int rows = goodMapper.updateTakeDown(ids, isTakeDown);
        evictHotCache();
        return rows;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int rows = goodMapper.deleteByIds(ids);
        evictHotCache();
        return rows;
    }

    /**
     * 批量扣减库存（下单链路）：逐条条件更新，任一商品库存不足/已下架即抛异常整体回滚，
     * 与订单侧的商品预检（在售 + 库存预检）互为基础，条件 SQL 保证并发下不超卖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deductStock(List<StockChangeParam> changes) {
        if (changes == null || changes.isEmpty()) {
            return 0;
        }
        int rows = 0;
        for (StockChangeParam change : changes) {
            if (change.getGoodId() == null || change.getCount() == null || change.getCount() < 1) {
                throw new BizException(ResponseCode.BAD_REQUEST, "库存扣减参数不合法");
            }
            int affected = goodMapper.deductStock(change.getGoodId(), change.getCount());
            if (affected == 0) {
                throw new BizException(ResponseCode.STOCK_NOT_ENOUGH,
                        "库存不足或商品已下架，商品 ID: " + change.getGoodId());
            }
            rows += affected;
        }
        return rows;
    }

    /** 批量回补库存（取消/超时关单）：宽松处理，商品不存在/参数缺失跳过该条并告警 */
    @Override
    public int restoreStock(List<StockChangeParam> changes) {
        if (changes == null || changes.isEmpty()) {
            return 0;
        }
        int rows = 0;
        for (StockChangeParam change : changes) {
            if (change.getGoodId() == null || change.getCount() == null || change.getCount() < 1) {
                continue;
            }
            int affected = goodMapper.restoreStock(change.getGoodId(), change.getCount());
            if (affected == 0) {
                log.warn("库存回补跳过（商品不存在）goodId={}", change.getGoodId());
            }
            rows += affected;
        }
        return rows;
    }

    /** 统计引用指定品牌的商品数（is_del=0，brand-api 删除品牌前校验） */
    @Override
    public int countByBrandId(Long brandId) {
        return brandId == null ? 0 : goodMapper.countByBrandId(brandId);
    }

    /** 统计引用指定分类的商品数（is_del=0，category-api 删除分类前校验） */
    @Override
    public int countByCategoryId(Long categoryId) {
        return categoryId == null ? 0 : goodMapper.countByCategoryId(categoryId);
    }

    /** 商品文本字段 XSS 过滤（名称/别名/摘要为纯文本；detail 为富文本详情，保留 HTML 不过滤） */
    private void sanitizeTextFields(GoodEntity good) {
        good.setName(XssUtil.stripTags(good.getName()));
        good.setAlias(XssUtil.stripTags(good.getAlias()));
        good.setSummary(XssUtil.stripTags(good.getSummary()));
    }

    /** 详情图覆盖式保存（goodId 已由主表回填） */
    private void saveDetailPics(GoodEntity good) {
        List<GoodDetailPicEntity> pics = good.getDetailPicList();
        if (pics == null || pics.isEmpty()) {
            return;
        }
        goodMapper.deleteDetailPicsByGoodId(good.getId());
        for (GoodDetailPicEntity pic : pics) {
            pic.setGoodId(good.getId());
        }
        goodMapper.saveDetailPics(pics);
    }
}
