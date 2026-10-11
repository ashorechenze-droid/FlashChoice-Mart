package com.example.fcmordermicroservice.listener;

import com.example.fcmordermicroservice.config.RabbitMqConfig;
import com.example.fcmspi.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 超时关单消息消费者：消费延时队列到期死信到关单队列的消息，
 * 调用幂等关单（仅待付款订单会被关闭）；异常抛出触发监听器重试，重试耗尽进入死信队列。
 */
@Slf4j
@Component
public class OrderCloseMessageListener {

    private final OrderService orderService;

    public OrderCloseMessageListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMqConfig.CLOSE_QUEUE)
    public void onCloseMessage(String orderNo) {
        log.info("收到超时关单消息 orderNo={}", orderNo);
        int rows = orderService.closeTimeoutOrder(orderNo);
        log.info("超时关单处理完成 orderNo={}, rows={}", orderNo, rows);
    }
}
