package com.example.fcmseckillmicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.SeckillOrderParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "FCM-order-microservice", path = "api/orders")
public interface OrderFeignClient {
    @PostMapping("/internal/seckill")
    TypedJsonResp<OrderEntity> createSeckillOrder(@RequestBody SeckillOrderParam param);
}
