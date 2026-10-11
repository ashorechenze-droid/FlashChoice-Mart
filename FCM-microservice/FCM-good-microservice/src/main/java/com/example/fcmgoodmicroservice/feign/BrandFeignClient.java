package com.example.fcmgoodmicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.BrandEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 品牌服务 Feign 客户端（good-api 组装商品品牌用）
 * 对应 FCM-brand-microservice 的 GET /api/brands/id/{id}
 */
@FeignClient(name = "FCM-brand-microservice", path = "api/brands")
public interface BrandFeignClient {

    /** 品牌详情（data 为 null 表示不存在） */
    @GetMapping("/id/{id}")
    TypedJsonResp<BrandEntity> getById(@PathVariable Long id);
}
