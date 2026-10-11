package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 商品收藏（collect，member_id + good_id 唯一，重复收藏幂等）
 */
@Data
public class CollectEntity extends AuditEntity {

    /** 会员 ID */
    private Long memberId;
    /** 商品 ID */
    private Long goodId;

    /** 非表字段：商品信息（列表回显） */
    private transient GoodEntity good;
}
