package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 用户组（t_rbac_group）
 */
@Data
public class RbacGroupEntity extends AuditEntity {

    /** 组名（唯一） */
    private String name;
    /** 是否启用 */
    private Boolean enabled;
    /** 说明 */
    private String description;
}
