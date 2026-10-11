package com.example.fcmgoodmicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.PriceDropNoticeParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 收藏服务（通知）Feign 客户端（good-api 改价下调后触发收藏者降价通知用）
 * 对应 FCM-collect-microservice 的内部接口 POST /api/notices/internal/price-drop
 */
@FeignClient(name = "FCM-collect-microservice", path = "api/notices/internal")
public interface CollectNoticeFeignClient {

    /** 降价通知：向收藏该商品的会员批量下发站内信（返回下发条数） */
    @PostMapping("/price-drop")
    TypedJsonResp<Integer> notifyPriceDrop(@RequestBody PriceDropNoticeParam param);
}
