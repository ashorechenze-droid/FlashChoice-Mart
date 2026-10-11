package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.util.List;

/**
 * 秒杀活动（seckill，状态由 enabled 与起止时间推导）
 */
@Data
public class SeckillEntity extends AuditEntity {

    /** 活动名称 */
    private String name;
    /** 是否启用 */
    private Boolean enabled;
    /** 开始时间（yyyy-MM-dd HH:mm:ss） */
    private String startTime;
    /** 结束时间（yyyy-MM-dd HH:mm:ss） */
    private String endTime;
    /** 说明 */
    private String description;

    /** 非表字段：活动商品列表 */
    private transient List<SeckillGoodEntity> seckillGoods;
}
