package com.example.fcmspi.model;

import lombok.Data;

/**
 * 库存变更参数（服务间 Feign：下单扣减 / 取消回补）
 */
@Data
public class StockChangeParam {

    /** 商品 ID */
    private Long goodId;
    /** 变动数量（扣减与回补均传正数） */
    private Integer count;
}
