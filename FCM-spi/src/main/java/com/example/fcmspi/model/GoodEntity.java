package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品（good，单品 SPU，无 SKU 体系）
 */
@Data
public class GoodEntity extends AuditEntity {

    /** 货号（唯一） */
    private String spuNo;
    /** 名称 */
    private String name;
    /** 别名 */
    private String alias;
    /** 摘要 */
    private String summary;
    /** 分类 ID */
    private Long categoryId;
    /** 品牌 ID */
    private Long brandId;
    /** 标价 */
    private BigDecimal markPrice;
    /** 实价 */
    private BigDecimal price;
    /** 库存 */
    private Integer qty;
    /** 主图 */
    private String pic;
    /** 次图 */
    private String pic2;
    /** 详情图 URL 集合 */
    private String detailPics;
    /** 富文本详情 */
    private String detail;
    /** 是否下架 */
    private Boolean isTakeDown;
    /** 是否热销 */
    private Boolean isHot;
    /** 逻辑删除 */
    private Boolean isDel;
    /** 是否秒杀商品 */
    private Boolean isSeckill;
    /** 备注 */
    private String description;

    /** 非表字段：品牌（详情 full=true 时经 Feign 组装） */
    private transient BrandEntity brand;
    /** 非表字段：分类（详情 full=true 时经 Feign 组装） */
    private transient CategoryEntity category;
    /** 非表字段：详情图列表（good_detail_pics） */
    private transient List<GoodDetailPicEntity> detailPicList;
}
