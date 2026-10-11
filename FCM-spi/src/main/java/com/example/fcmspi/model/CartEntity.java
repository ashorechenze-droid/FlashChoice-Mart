package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 购物车（cart，member_id + good_id 唯一，重复加购累加数量）
 */
@Data
public class CartEntity extends AuditEntity {

    /** 会员 ID */
    private Long memberId;
    /** 商品 ID */
    private Long goodId;
    /** 数量 */
    private Integer qty;

    /** 非表字段：商品信息（列表回显） */
    private transient GoodEntity good;
}
