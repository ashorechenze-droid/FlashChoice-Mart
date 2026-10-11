package com.example.fcmspi.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 进入秒杀通知参数（服务间 Feign：seckill-api 商品加入活动后，调用 collect-api 触发）
 */
@Data
public class SeckillJoinNoticeParam {

    /** 商品 ID */
    private Long goodId;
    /** 商品名称（消息展示用，可为空） */
    private String goodName;
    /** 秒杀活动名称 */
    private String seckillName;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
}
