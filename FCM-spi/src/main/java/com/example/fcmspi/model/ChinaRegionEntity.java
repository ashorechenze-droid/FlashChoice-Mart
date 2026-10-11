package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.util.List;

/**
 * 行政区划（t_china_region，省/市/区三级树）
 */
@Data
public class ChinaRegionEntity extends AuditEntity {

    /** 名称 */
    private String name;
    /** 父级 ID */
    private Long parentId;
    /** 排序 */
    private Integer sortOrder;
    /** 层级（1 省 / 2 市 / 3 区） */
    private Integer level;
    /** 说明 */
    private String description;

    /** 非表字段：子级区域（树） */
    private transient List<ChinaRegionEntity> children;
}
