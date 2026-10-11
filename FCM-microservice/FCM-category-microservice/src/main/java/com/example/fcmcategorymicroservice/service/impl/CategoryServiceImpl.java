package com.example.fcmcategorymicroservice.service.impl;

import com.example.fcmcategorymicroservice.feign.GoodFeignClient;
import com.example.fcmcategorymicroservice.mapper.CategoryMapper;
import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmcommon.util.JsonUtil;
import com.example.fcmspi.model.CategoryEntity;
import com.example.fcmspi.service.CategoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    /** 分类树缓存 key（存扁平列表 JSON；children 为 transient 字段，读回后内存组树） */
    private static final String CACHE_KEY_TREE = "fcm:cache:category:tree";
    /** 缓存有效期（分类低频变更，短 TTL + 变更主动失效双重兜底） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final CategoryMapper categoryMapper;
    private final GoodFeignClient goodFeignClient;
    private final StringRedisTemplate redisTemplate;

    public CategoryServiceImpl(CategoryMapper categoryMapper,
                               GoodFeignClient goodFeignClient,
                               StringRedisTemplate redisTemplate) {
        this.categoryMapper = categoryMapper;
        this.goodFeignClient = goodFeignClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<CategoryEntity> listAll() {
        return categoryMapper.findAll(null);
    }

    @Override
    public List<CategoryEntity> listByParentId(Long parentId) {
        return categoryMapper.findAll(parentId);
    }

    @Override
    public List<CategoryEntity> tree() {
        // 先读缓存（存扁平列表），未命中回源数据库并回填；Redis 异常时直接回源，不影响可用性
        List<CategoryEntity> all = readTreeCache();
        if (all == null) {
            all = categoryMapper.findAll(null);
            writeTreeCache(all);
        }
        return buildTree(all, 0L);
    }

    /** 读分类树缓存；未命中或 Redis 异常返回 null（由调用方回源数据库） */
    private List<CategoryEntity> readTreeCache() {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY_TREE);
            if (json != null) {
                return JsonUtil.fromJson(json, new TypeReference<List<CategoryEntity>>() { });
            }
        } catch (Exception e) {
            log.warn("分类树缓存读取失败，回源数据库: {}", e.getMessage());
        }
        return null;
    }

    /** 写分类树缓存（失败仅告警，不阻断查询） */
    private void writeTreeCache(List<CategoryEntity> all) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY_TREE, JsonUtil.toJson(all), CACHE_TTL);
        } catch (Exception e) {
            log.warn("分类树缓存写入失败: {}", e.getMessage());
        }
    }

    /** 分类变更后失效缓存，下次查询回源重建（失败仅告警，TTL 到期后自然收敛） */
    private void evictTreeCache() {
        try {
            redisTemplate.delete(CACHE_KEY_TREE);
        } catch (Exception e) {
            log.warn("分类树缓存失效失败: {}", e.getMessage());
        }
    }

    /** 内存递归组装分类树（根为 parent_id=0，children 为 transient 字段） */
    private List<CategoryEntity> buildTree(List<CategoryEntity> all, Long parentId) {
        List<CategoryEntity> nodes = new ArrayList<>();
        for (CategoryEntity category : all) {
            if (Objects.equals(parentId, category.getParentId())) {
                category.setChildren(buildTree(all, category.getId()));
                nodes.add(category);
            }
        }
        return nodes;
    }

    @Override
    public CategoryEntity getById(Long id) {
        return categoryMapper.findById(id);
    }

    @Override
    public int insert(CategoryEntity category) {
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        category.setCreatedTime(now);
        category.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        int rows = categoryMapper.save(category);
        evictTreeCache();
        return rows;
    }

    @Override
    public int update(CategoryEntity category) {
        category.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN)));
        int rows = categoryMapper.update(category);
        evictTreeCache();
        return rows;
    }

    @Override
    public int deleteById(Long id) {
        if (categoryMapper.countByParentId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "存在子分类，无法删除");
        }
        // 删除前校验商品引用
        if (countGoods(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "分类下存在商品，无法删除");
        }
        int rows = categoryMapper.deleteById(id);
        evictTreeCache();
        return rows;
    }

    /** 商品引用计数；远程不可用时阻断删除，避免误删仍被引用的分类 */
    private int countGoods(Long categoryId) {
        try {
            TypedJsonResp<Integer> resp = goodFeignClient.countByCategoryId(categoryId);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                throw new BizException(ResponseCode.BAD_REQUEST, "商品服务不可用，暂时无法删除分类");
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败 categoryId={}: {}", categoryId, e.getMessage());
            throw new BizException(ResponseCode.BAD_REQUEST, "商品服务不可用，暂时无法删除分类");
        }
    }
}
