package com.example.fcmordermicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.MemberAddressEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 会员地址服务 Feign 客户端（order-api 下单收货地址快照用）
 * 对应 FCM-member-address-microservice 的 GET /api/member-addresses/id/{id}
 */
@FeignClient(name = "FCM-member-address-microservice", path = "api/member-addresses")
public interface MemberAddressFeignClient {

    /** 地址详情（data 为 null 表示地址不存在） */
    @GetMapping("/id/{id}")
    TypedJsonResp<MemberAddressEntity> getById(@PathVariable("id") Long id);
}
