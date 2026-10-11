package com.example.fcmordermicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.CartEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 购物车服务 Feign 客户端（order-api 购物车结算取条目/下单成功清除用）
 * 对应 FCM-cart-microservice 的 GET /api/carts/ids、DELETE /api/carts
 */
@FeignClient(name = "FCM-cart-microservice", path = "api/carts")
public interface CartFeignClient {

    /** 按 ID 批量查询购物车条目（下单结算用；不存在的 id 不会出现在结果中，归属由订单侧校验） */
    @GetMapping("/ids")
    TypedJsonResp<List<CartEntity>> listByIds(@RequestParam("ids") List<Long> ids);

    /** 批量删除购物车条目（下单成功后清除；memberId 参与归属校验，非本人条目不会被删除） */
    @DeleteMapping
    TypedJsonResp<Integer> deleteByIds(@RequestParam("memberId") Long memberId,
                                       @RequestBody List<Long> ids);
}
