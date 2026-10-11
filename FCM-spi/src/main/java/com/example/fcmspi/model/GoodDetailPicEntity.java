package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 商品详情图片（good_detail_pics）
 */
@Data
public class GoodDetailPicEntity extends AuditEntity {

    /** 商品 ID */
    private Long goodId;
    /** 图片 URL */
    private String url;
    /** 排序 */
    private Integer sort;
    /** 说明 */
    private String description;
}
