package com.example.fcmordermicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "FCM-seckill-microservice", path = "api/seckills")
public interface SeckillFeignClient {
    @PutMapping("/internal/release/{seckillNo}")
    TypedJsonResp<Integer> release(@PathVariable("seckillNo") String seckillNo);
}
