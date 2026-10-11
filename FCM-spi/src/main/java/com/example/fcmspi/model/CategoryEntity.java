package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.util.List;

/**
 * 商品分类（category，parent_id 自关联成树）
 */
@Data
public class CategoryEntity extends AuditEntity {

    /** 父分类 ID（0 为根） */
    private Long parentId;
    /** 名称 */
    private String name;
    /** 标题 */
    private String title;
    /** 标签 */
    private String tag;
    /** 图标 */
    private String icon;
    /** 摘要 */
    private String summary;
    /** 排序 */
    private Integer sort;
    /** 简介 */
    private String description;

    /** 非表字段：子分类（树） */
    private transient List<CategoryEntity> children;
}
