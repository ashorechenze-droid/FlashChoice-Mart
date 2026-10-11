package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀活动商品（seckill_good，seckill_id + good_id 唯一）
 */
@Data
public class SeckillGoodEntity extends AuditEntity {

    /** 秒杀活动 ID */
    private Long seckillId;
    /** 商品 ID */
    private Long goodId;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 秒杀库存 */
    private Integer stock;
    /** 已售数量 */
    private Integer sold;
    /** 每人限购数 */
    private Integer limitPerUser;
    /** 说明 */
    private String description;

    /** 非表字段：商品信息（列表回显） */
    private transient GoodEntity good;
}
