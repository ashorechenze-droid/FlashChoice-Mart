package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 角色-权限关联（t_rbac_role_perm，role_id + perm_id 唯一）
 */
@Data
public class RbacRolePermEntity extends AuditEntity {

    /** 角色 ID */
    private Long roleId;
    /** 权限 ID */
    private Long permId;
}
