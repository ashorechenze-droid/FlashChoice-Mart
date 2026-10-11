package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 会员通知（notice，站内信：收藏商品降价 / 进入秒杀触达）
 */
@Data
public class NoticeEntity extends AuditEntity {

    /** 通知类型：商品降价 */
    public static final int TYPE_PRICE_DROP = 1;
    /** 通知类型：进入秒杀会场 */
    public static final int TYPE_SECKILL_JOIN = 2;

    /** 接收会员 ID */
    private Long memberId;
    /** 通知类型（1降价 2进入秒杀） */
    private Integer type;
    /** 关联商品 ID */
    private Long goodId;
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
    /** 是否已读（0未读/1已读） */
    private Boolean isRead;
}
