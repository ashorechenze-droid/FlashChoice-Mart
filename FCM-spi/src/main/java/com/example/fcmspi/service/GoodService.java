package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.StockChangeParam;
import com.example.fcmspi.model.search.GoodSearchBean;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 商品业务接口（good-api）：分页检索、详情、CRUD、上下架、逻辑删除、库存扣减/回补
 */
public interface GoodService {

    /** 前台/后台共用的分页检索（关键字/分类/品牌/价格区间/排序/热销） */
    PageInfo<GoodEntity> page(GoodSearchBean search, PaginateInfo page);

    /** 基本信息 */
    GoodEntity getById(Long id);

    /** 详情（full=true）：经 BrandFeignService / CategoryFeignService 组装品牌、分类与详情图 */
    GoodEntity getDetail(Long id);

    /** 按 ID 批量查询（下单快照/购物车回显等） */
    List<GoodEntity> listByIds(List<Long> ids);

    int insert(GoodEntity good);

    int update(GoodEntity good);

    /** 批量上/下架 */
    int updateTakeDown(List<Long> ids, Boolean isTakeDown);

    /** 批量逻辑删除（is_del=1） */
    int deleteByIds(List<Long> ids);

    /** 批量扣减库存（条件更新 qty >= count；任一不足/商品不可售即整体失败回滚，下单链路使用） */
    int deductStock(List<StockChangeParam> changes);

    /** 批量回补库存（取消/超时关单回补；商品不存在时跳过该条） */
    int restoreStock(List<StockChangeParam> changes);

    /** 统计引用指定品牌的商品数（is_del=0，brand-api 删除品牌前校验引用） */
    int countByBrandId(Long brandId);

    /** 统计引用指定分类的商品数（is_del=0，category-api 删除分类前校验引用） */
    int countByCategoryId(Long categoryId);
}
