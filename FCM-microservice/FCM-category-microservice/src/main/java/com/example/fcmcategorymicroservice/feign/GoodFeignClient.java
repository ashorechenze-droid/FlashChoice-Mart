package com.example.fcmcategorymicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端（category-api 删除分类前校验商品引用）
 * 对应 FCM-good-microservice 的 GET /api/goods/count
 */
@FeignClient(name = "FCM-good-microservice", path = "api/goods")
public interface GoodFeignClient {

    /** 统计引用指定分类的商品数（data 为数量） */
    @GetMapping("/count")
    TypedJsonResp<Integer> countByCategoryId(@RequestParam("categoryId") Long categoryId);
}
