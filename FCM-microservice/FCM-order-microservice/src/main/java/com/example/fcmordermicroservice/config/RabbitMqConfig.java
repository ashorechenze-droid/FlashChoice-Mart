package com.example.fcmordermicroservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 超时关单队列（TTL + 死信队列实现 30 分钟延时投递）
 *
 * <p>链路：生产者投递 → order.delay.queue（无消费者，TTL 30 分钟到期后死信）
 * → order.close.exchange → order.close.queue（消费者执行幂等关单）
 * → 消费重试耗尽仍失败 → order.close.dlx.queue（死信队列，人工兜底）。
 * 定时扫描（closeTimeoutOrders）保留作为最终兜底。
 * <p>队列/交换机均由 RabbitAdmin 在连接建立时自动声明（幂等）。
 */
@Configuration
public class RabbitMqConfig {

    /* ===== 延时队列（生产端投递入口） ===== */
    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String DELAY_ROUTING_KEY = "order.delay";

    /* ===== 关单队列（延时消息死信目标，消费端监听） ===== */
    public static final String CLOSE_EXCHANGE = "order.close.exchange";
    public static final String CLOSE_QUEUE = "order.close.queue";
    public static final String CLOSE_ROUTING_KEY = "order.close";

    /* ===== 关单死信队列（消费重试耗尽后的人工兜底） ===== */
    public static final String CLOSE_DLX_EXCHANGE = "order.close.dlx.exchange";
    public static final String CLOSE_DLX_QUEUE = "order.close.dlx.queue";
    public static final String CLOSE_DLX_ROUTING_KEY = "order.close.dlx";

    /** 延时 TTL：30 分钟（与定时扫描兜底的时间窗一致） */
    private static final int DELAY_TTL_MILLIS = 30 * 60 * 1000;

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    /** 延时队列：消息到期后死信到关单交换机 */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .ttl(DELAY_TTL_MILLIS)
                .deadLetterExchange(CLOSE_EXCHANGE)
                .deadLetterRoutingKey(CLOSE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange closeExchange() {
        return new DirectExchange(CLOSE_EXCHANGE, true, false);
    }

    /** 关单队列：消费重试耗尽后死信到死信队列 */
    @Bean
    public Queue closeQueue() {
        return QueueBuilder.durable(CLOSE_QUEUE)
                .deadLetterExchange(CLOSE_DLX_EXCHANGE)
                .deadLetterRoutingKey(CLOSE_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding closeBinding() {
        return BindingBuilder.bind(closeQueue()).to(closeExchange()).with(CLOSE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange closeDlxExchange() {
        return new DirectExchange(CLOSE_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue closeDlxQueue() {
        return QueueBuilder.durable(CLOSE_DLX_QUEUE).build();
    }

    @Bean
    public Binding closeDlxBinding() {
        return BindingBuilder.bind(closeDlxQueue()).to(closeDlxExchange()).with(CLOSE_DLX_ROUTING_KEY);
    }
}
