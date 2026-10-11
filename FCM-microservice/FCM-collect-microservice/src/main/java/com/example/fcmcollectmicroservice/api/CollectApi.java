package com.example.fcmcollectmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.CollectEntity;
import com.example.fcmspi.service.CollectService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/collects")
public class CollectApi {

    private final CollectService collectService;

    public CollectApi(CollectService collectService) {
        this.collectService = collectService;
    }

    /** 解析网关注入的会员 ID 请求头（X-User-Id），缺省时取查询参数 memberId */
    private Long memberId(String headerMemberId, Long paramMemberId) {
        if (headerMemberId != null) {
            try {
                return Long.valueOf(headerMemberId);
            } catch (NumberFormatException ignored) {
                // 头非法时回退查询参数
            }
        }
        return paramMemberId;
    }

    /** 会员专属操作：后台令牌直接 403（收藏数据为会员私有） */
    private void requireMember(String audience) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限会员");
        }
    }

    /** 当前会员收藏列表（商品信息经 good-api Feign 回显，服务不可用时降级为 null） */
    @GetMapping
    public JsonResp list(@RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                         @RequestParam(required = false) Long memberId) {
        return JsonResp.ok(collectService.listByMemberId(memberId(headerMemberId, memberId)));
    }

    /** 当前会员是否已收藏该商品（商品详情页/卡片星标状态用） */
    @GetMapping("/check")
    public JsonResp check(@RequestParam Long goodId,
                          @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                          @RequestParam(required = false) Long memberId) {
        return JsonResp.ok(collectService.isCollected(memberId(headerMemberId, memberId), goodId));
    }

    /** 收藏商品：member_id + good_id 已存在则幂等返回 */
    @PostMapping
    public JsonResp add(@RequestBody CollectEntity collect,
                        @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                        @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId) {
        requireMember(audience);
        if (collect.getMemberId() == null && headerMemberId != null) {
            collect.setMemberId(memberId(headerMemberId, null));
        }
        return JsonResp.ok(collectService.add(collect));
    }

    /** 按商品取消收藏（详情页/商品卡片「取消收藏」用） */
    @DeleteMapping("/good/{goodId}")
    public JsonResp deleteByGoodId(@PathVariable Long goodId,
                                   @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                   @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                                   @RequestParam(required = false) Long memberId) {
        requireMember(audience);
        return JsonResp.ok(collectService.deleteByGoodId(memberId(headerMemberId, memberId), goodId));
    }

    /** 批量取消收藏（校验归属） */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids,
                                @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                                @RequestParam(required = false) Long memberId) {
        requireMember(audience);
        return JsonResp.ok(collectService.deleteByIds(memberId(headerMemberId, memberId), ids));
    }
}
