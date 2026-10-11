package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 资源（t_rbac_resource，type + value 唯一）
 */
@Data
public class RbacResourceEntity extends AuditEntity {

    /** 资源名 */
    private String name;
    /** 类型（接口 URL / 按钮标识） */
    private String type;
    /** 路径或按钮标识 */
    private String value;
    /** 说明 */
    private String description;
}
