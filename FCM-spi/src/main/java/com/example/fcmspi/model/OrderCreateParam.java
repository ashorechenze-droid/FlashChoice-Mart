package com.example.fcmspi.model;

import lombok.Data;

import java.util.List;

/**
 * 下单入参（非表对象）：支持购物车结算（cartIds）与立即购买（goodId + qty）两种方式
 */
@Data
public class OrderCreateParam {

    /** 下单会员账号（由登录上下文注入） */
    private String memberAccount;
    /** 下单会员 ID（网关身份头 X-User-Id 注入；购物车结算的归属校验与清除用，非落库字段） */
    private Long memberId;
    /** 购物车勾选条目 ID 列表（购物车结算） */
    private List<Long> cartIds;
    /** 商品 ID（立即购买） */
    private Long goodId;
    /** 数量（立即购买） */
    private Integer qty;
    /** 收货地址 ID */
    private Long addrId;
    /** 订单备注 */
    private String comment;
}
