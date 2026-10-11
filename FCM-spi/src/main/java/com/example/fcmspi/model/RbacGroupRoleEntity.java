package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 组-角色关联（t_rbac_group_role，group_id + role_id 唯一）
 */
@Data
public class RbacGroupRoleEntity extends AuditEntity {

    /** 用户组 ID */
    private Long groupId;
    /** 角色 ID */
    private Long roleId;
}
