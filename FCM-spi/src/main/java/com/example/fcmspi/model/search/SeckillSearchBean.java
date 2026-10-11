package com.example.fcmspi.model.search;

import com.example.fcmspi.model.SeckillEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀活动查询条件：支持名称关键字与 enabled 等继承字段过滤
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillSearchBean extends SeckillEntity {

    /** 关键字：匹配 name */
    private String keyword;
}
