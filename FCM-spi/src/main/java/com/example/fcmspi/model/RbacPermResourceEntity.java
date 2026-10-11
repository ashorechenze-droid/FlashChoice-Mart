package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 权限-资源关联（t_rbac_perm_resource，resource_id + perm_id 唯一）
 */
@Data
public class RbacPermResourceEntity extends AuditEntity {

    /** 资源 ID */
    private Long resourceId;
    /** 权限 ID */
    private Long permId;
}
