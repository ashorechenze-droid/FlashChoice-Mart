package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 权限（t_rbac_perm）
 */
@Data
public class RbacPermEntity extends AuditEntity {

    /** 权限名（唯一） */
    private String name;
    /** 是否启用 */
    private Boolean enabled;
    /** 说明 */
    private String description;
}
