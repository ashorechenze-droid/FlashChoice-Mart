package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细（order_item，商品快照）
 */
@Data
public class OrderItemEntity extends AuditEntity {

    /** 订单 ID */
    private Long orderId;
    /** 商品 ID */
    private Long goodId;
    /** 成交价 */
    private BigDecimal dealPrice;
    /** 数量 */
    private Integer count;
    /** 商品名称（快照） */
    private String goodName;
    /** 商品主图（快照） */
    private String goodPic;
    /** 商品描述（快照） */
    private String goodDesc;
}
