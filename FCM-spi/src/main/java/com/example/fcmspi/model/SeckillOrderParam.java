package com.example.fcmspi.model;

import lombok.Data;
import java.math.BigDecimal;

/** Internal message and order creation contract. Never accepted by the public order endpoint. */
@Data
public class SeckillOrderParam {
    private String seckillNo;
    private Long seckillGoodId;
    private Long goodId;
    private Long memberId;
    private String memberAccount;
    private Long addrId;
    private BigDecimal seckillPrice;
}
