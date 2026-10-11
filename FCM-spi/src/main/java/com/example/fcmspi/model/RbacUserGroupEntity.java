package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 用户-组关联（t_rbac_user_group，user_id + group_id 唯一）
 */
@Data
public class RbacUserGroupEntity extends AuditEntity {

    /** 用户 ID */
    private Long userId;
    /** 用户组 ID */
    private Long groupId;
}
