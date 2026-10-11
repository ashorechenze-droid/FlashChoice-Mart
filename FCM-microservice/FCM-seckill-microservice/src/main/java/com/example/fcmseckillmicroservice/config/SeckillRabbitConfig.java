package com.example.fcmseckillmicroservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeckillRabbitConfig {
    public static final String EXCHANGE = "seckill.order.exchange";
    public static final String QUEUE = "seckill.order.queue";
    public static final String ROUTING_KEY = "seckill.order";
    public static final String DEAD_EXCHANGE = "seckill.order.dead.exchange";
    public static final String DEAD_QUEUE = "seckill.order.dead.queue";

    @Bean DirectExchange seckillExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean Queue seckillQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY).build();
    }
    @Bean Binding seckillBinding() { return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(ROUTING_KEY); }
    @Bean DirectExchange seckillDeadExchange() { return new DirectExchange(DEAD_EXCHANGE, true, false); }
    @Bean Queue seckillDeadQueue() { return QueueBuilder.durable(DEAD_QUEUE).build(); }
    @Bean Binding seckillDeadBinding() {
        return BindingBuilder.bind(seckillDeadQueue()).to(seckillDeadExchange()).with(ROUTING_KEY);
    }
}
