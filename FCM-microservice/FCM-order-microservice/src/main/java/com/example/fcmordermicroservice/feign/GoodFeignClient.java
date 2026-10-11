package com.example.fcmordermicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.StockChangeParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品服务 Feign 客户端（order-api 下单快照/库存扣减/取消回补用）
 * 对应 FCM-good-microservice 的 GET /api/goods/ids、PUT /api/goods/stock/deduct、PUT /api/goods/stock/restore
 */
@FeignClient(name = "FCM-good-microservice", path = "api/goods")
public interface GoodFeignClient {

    /** 按 ID 批量查询商品（明细快照组装与在售/库存预检用；不存在的 id 不会出现在结果中） */
    @GetMapping("/ids")
    TypedJsonResp<List<GoodEntity>> listByIds(@RequestParam("ids") List<Long> ids);

    /** 批量扣减库存（条件更新 qty >= count；不足/已下架返回 success=false，服务端整体回滚） */
    @PutMapping("/stock/deduct")
    TypedJsonResp<Integer> deductStock(@RequestBody List<StockChangeParam> changes);

    /** 批量回补库存（取消/超时关单回补；商品不存在时跳过该条） */
    @PutMapping("/stock/restore")
    TypedJsonResp<Integer> restoreStock(@RequestBody List<StockChangeParam> changes);
}
