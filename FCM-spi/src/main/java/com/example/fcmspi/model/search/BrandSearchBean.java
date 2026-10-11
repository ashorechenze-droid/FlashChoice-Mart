package com.example.fcmspi.model.search;

import com.example.fcmspi.model.BrandEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌查询条件：支持关键字（名称/公司模糊）与继承字段过滤
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandSearchBean extends BrandEntity {

    /** 关键字：匹配 name / company */
    private String keyword;
}
