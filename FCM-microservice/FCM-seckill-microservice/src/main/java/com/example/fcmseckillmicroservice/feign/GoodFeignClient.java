package com.example.fcmseckillmicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.GoodEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品服务 Feign 客户端（seckill-api 秒杀活动商品信息回显用）
 * 对应 FCM-good-microservice 的 GET /api/goods/ids
 */
@FeignClient(name = "FCM-good-microservice", path = "api/goods")
public interface GoodFeignClient {

    /** 按 ID 批量查询商品（列表回显用；不存在的 id 不会出现在结果中） */
    @GetMapping("/ids")
    TypedJsonResp<List<GoodEntity>> listByIds(@RequestParam("ids") List<Long> ids);
}
