package com.example.fcmseckillmicroservice.listener;

import com.example.fcmseckillmicroservice.config.SeckillRabbitConfig;
import com.example.fcmseckillmicroservice.service.impl.SeckillServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderListener {
    private final SeckillServiceImpl service;
    public SeckillOrderListener(SeckillServiceImpl service) { this.service = service; }

    @RabbitListener(queues = SeckillRabbitConfig.QUEUE)
    public void onOrder(String seckillNo) {
        service.processGrab(seckillNo);
    }
}
