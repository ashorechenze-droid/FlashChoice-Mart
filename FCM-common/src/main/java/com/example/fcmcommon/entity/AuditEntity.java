package com.example.fcmcommon.entity;

import lombok.Data;

/**
 * 审计实体基类：统一主键 id 与审计字段
 * （createdBy / createdTime / updatedBy / updatedTime，时间格式 yyyy-MM-dd HH:mm:ss）
 */
@Data
public abstract class AuditEntity implements Identity<Long> {

    /** 主键（BIGINT 自增） */
    private Long id;
    /** 创建人 */
    private String createdBy;
    /** 创建时间（yyyy-MM-dd HH:mm:ss） */
    private String createdTime;
    /** 更新人 */
    private String updatedBy;
    /** 更新时间（yyyy-MM-dd HH:mm:ss） */
    private String updatedTime;
}
