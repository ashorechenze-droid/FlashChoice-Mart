package com.example.fcmspi.model.search;

import com.example.fcmspi.model.OrderEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单查询条件：支持关键字、下单时间范围与 status / memberAccount 等继承字段过滤
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderSearchBean extends OrderEntity {

    /** 关键字：匹配 orderNo */
    private String keyword;
    /** 下单时间起（yyyy-MM-dd HH:mm:ss，created_time >=） */
    private String beginTime;
    /** 下单时间止（yyyy-MM-dd HH:mm:ss，created_time <=） */
    private String endTime;
}
