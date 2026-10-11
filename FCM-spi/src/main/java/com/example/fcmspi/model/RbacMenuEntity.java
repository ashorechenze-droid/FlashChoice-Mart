package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.util.List;

/**
 * 菜单（t_rbac_menu，parent_id 自关联成树，resource_id 关联资源）
 */
@Data
public class RbacMenuEntity extends AuditEntity {

    /** 父菜单 ID */
    private Long parentId;
    /** 关联资源 ID */
    private Long resourceId;
    /** 菜单名 */
    private String name;
    /** 图标 */
    private String icon;
    /** 菜单 URL */
    private String url;
    /** 排序 */
    private Integer sort;
    /** 说明 */
    private String description;

    /** 非表字段：子菜单（树） */
    private transient List<RbacMenuEntity> children;
}
