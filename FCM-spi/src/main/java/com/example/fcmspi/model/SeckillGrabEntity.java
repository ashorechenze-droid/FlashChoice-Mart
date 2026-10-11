package com.example.fcmspi.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeckillGrabEntity {
    private String seckillNo;
    private Long seckillGoodId;
    private Long memberId;
    private String memberAccount;
    private Long addrId;
    private BigDecimal seckillPrice;
    private String status;
    private String orderNo;
}
