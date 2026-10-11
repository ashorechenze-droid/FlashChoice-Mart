package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 商品品牌（brand）
 */
@Data
public class BrandEntity extends AuditEntity {

    /** 品牌名称（唯一） */
    private String name;
    /** 公司名称 */
    private String company;
    /** Logo URL */
    private String logo;
    /** 网址 */
    private String site;
    /** 简介 */
    private String description;
}
