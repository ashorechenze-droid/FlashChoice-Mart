package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.OrderCreateParam;
import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.SeckillOrderParam;
import com.example.fcmspi.model.search.OrderSearchBean;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 订单业务接口（order-api）：下单、查询、状态机、模拟支付/退款、超时关单
 * 状态机：待付款→(支付)→已支付→(发货)→待收货→(确认收货)→已确认；待付款可取消/超时自动关闭
 */
public interface OrderService {

    /** 分页查询（会员传 memberAccount；后台全量按状态/时间筛选） */
    PageInfo<OrderEntity> page(OrderSearchBean search, PaginateInfo page);

    /** 订单详情（withItems=true 时含明细） */
    OrderEntity getById(Long id, boolean withItems);

    OrderEntity getByOrderNo(String orderNo, boolean withItems);

    /**
     * 下单（事务）：校验商品→生成订单（雪花 ID）与明细（商品快照）→扣减库存→返回订单；
     * 购物车勾选清除由实现层异步处理
     */
    OrderEntity createOrder(OrderCreateParam param);

    /** Internal idempotent creation at the reserved seckill price. */
    OrderEntity createSeckillOrder(SeckillOrderParam param);

    /** 模拟支付确认：校验归属与金额→写交易号与支付时间→置为已支付（幂等） */
    int payConfirm(Long id, String memberAccount, String payType);

    /** 取消订单（待付款，会员本人/超时场景），回补库存 */
    int cancel(Long id, String memberAccount);

    /** 后台发货（已支付→待收货） */
    int ship(Long id);

    /** 会员确认收货（待收货→已确认） */
    int confirmReceive(Long id, String memberAccount);

    /** 发起退款（无→退款中，模拟退款；memberAccount 非空时校验订单归属，后台传 null 跳过） */
    int applyRefund(Long id, String refundReason, String memberAccount);

    /** 后台确认退款（退款中→已退款） */
    int confirmRefund(Long id);

    /** 关闭单笔超时订单（RabbitMQ 延时消息消费，回补库存） */
    int closeTimeoutOrder(String orderNo);

    /** 定时扫描兜底：关闭超时待付款订单 */
    void closeTimeoutOrders();

    /** 后台逻辑删除 */
    int deleteByIds(List<Long> ids);
}
