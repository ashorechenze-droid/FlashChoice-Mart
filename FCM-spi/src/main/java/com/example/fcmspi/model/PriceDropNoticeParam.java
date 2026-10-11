package com.example.fcmspi.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 降价通知参数（服务间 Feign：good-api 改价后检测到价格下调，调用 collect-api 触发）
 */
@Data
public class PriceDropNoticeParam {

    /** 商品 ID */
    private Long goodId;
    /** 商品名称（消息展示用；为空时回退旧名） */
    private String goodName;
    /** 原价 */
    private BigDecimal oldPrice;
    /** 现价 */
    private BigDecimal newPrice;
}
