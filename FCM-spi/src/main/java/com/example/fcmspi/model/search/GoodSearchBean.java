package com.example.fcmspi.model.search;

import com.example.fcmspi.model.GoodEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品查询条件（详见详细设计文档 4.6.1）：
 * 关键字/分类/品牌/价格区间/排序/热销，上架与未删除条件由实现层统一附加
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodSearchBean extends GoodEntity {

    /** 关键字：匹配 name / alias / spuNo */
    private String keyword;
    /** 价格下限 */
    private BigDecimal priceLow;
    /** 价格上限 */
    private BigDecimal priceHigh;
    /** 排序方式：default / priceAsc / priceDesc / hot */
    private String sort;

    /**
     * 非表字段：分类 ID 列表（含当前分类及其所有递归子分类）。
     * 由 Service 层调用分类服务 tree 接口后填充，Mapper 用 IN 查询；
     * 为空时回退到 categoryId 精确匹配。
     */
    private transient List<Long> categoryIdList;
}
