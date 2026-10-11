package com.example.fcmspi.model;

import lombok.Data;

/**
 * 秒杀抢购结果（非表对象，供前端轮询）
 */
@Data
public class GrabResult {

    /** 秒杀流水号/订单号 */
    private String orderNo;
    /** 抢购状态：处理中 / 成功 / 失败 */
    private String status;
    /** Real order number after asynchronous creation. */
    private String actualOrderNo;
}
