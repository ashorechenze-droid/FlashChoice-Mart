package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单（order，状态机见详细设计文档 3.5.1）
 */
@Data
public class OrderEntity extends AuditEntity {

    /** 订单号（雪花 ID，唯一） */
    private String orderNo;
    /** 秒杀流水号（秒杀订单填写） */
    private String seckillNo;
    /** 下单会员账号 */
    private String memberAccount;
    /** 应付总额 */
    private BigDecimal totalPay;
    /** 支付方式（模拟支付） */
    private String payType;
    /** 支付交易号（模拟支付流水号） */
    private String alipayTradeNo;
    /** 下单时间 */
    private String checkoutTime;
    /** 支付时间 */
    private String payTime;
    /** 发货时间 */
    private String shipTime;
    /** 收货时间 */
    private String acceptTime;
    /** 订单状态（待付款/已支付/待收货/已确认/已取消） */
    private String status;
    /** 收货地址 ID（快照来源） */
    private Long receiverAddrId;
    /** 收货人（快照） */
    private String receiverName;
    /** 收货人电话（快照） */
    private String receiverPhone;
    /** 收货地址（快照） */
    private String receiverAddrDetail;
    /** 订单备注 */
    private String orderComment;
    /** 退款状态（无/退款中/已退款，模拟退款用） */
    private String refundStatus;
    /** 逻辑删除 */
    private Boolean isDel;
    /** 备注 */
    private String description;

    /** 非表字段：订单明细 */
    private transient List<OrderItemEntity> orderItems;
}
