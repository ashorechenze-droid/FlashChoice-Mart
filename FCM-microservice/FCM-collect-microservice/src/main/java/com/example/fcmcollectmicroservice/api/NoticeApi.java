package com.example.fcmcollectmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.PriceDropNoticeParam;
import com.example.fcmspi.model.SeckillJoinNoticeParam;
import com.example.fcmspi.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/notices")
public class NoticeApi {

    private final NoticeService noticeService;

    public NoticeApi(NoticeService noticeService) {
        this.noticeService = noticeService;
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

    /** 会员专属操作：后台令牌直接 403（通知数据为会员私有） */
    private void requireMember(String audience) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限会员");
        }
    }

    /** 当前会员通知列表（?unreadOnly=true 仅未读，默认全部） */
    @GetMapping
    public JsonResp list(@RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
                         @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                         @RequestParam(required = false) Long memberId) {
        return JsonResp.ok(noticeService.listByMemberId(memberId(headerMemberId, memberId), unreadOnly));
    }

    /** 未读数量（消息中心红点） */
    @GetMapping("/unread-count")
    public JsonResp unreadCount(@RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                                @RequestParam(required = false) Long memberId) {
        return JsonResp.ok(noticeService.countUnread(memberId(headerMemberId, memberId)));
    }

    /** 单条标记已读（校验归属） */
    @PutMapping("/{id}/read")
    public JsonResp markRead(@PathVariable Long id,
                             @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                             @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                             @RequestParam(required = false) Long memberId) {
        requireMember(audience);
        return JsonResp.ok(noticeService.markRead(memberId(headerMemberId, memberId), id));
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public JsonResp markAllRead(@RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                                @RequestParam(required = false) Long memberId) {
        requireMember(audience);
        return JsonResp.ok(noticeService.markAllRead(memberId(headerMemberId, memberId)));
    }

    /* ===== 内部接口：仅供服务间 Feign 直连（good-api 改价下调 / seckill-api 加入活动触发），
       外部经网关访问会被 fcm.gateway.internal-paths 规则直接拒绝（403） ===== */

    /** 降价通知：向收藏该商品的会员批量下发站内信 */
    @PostMapping("/internal/price-drop")
    public JsonResp notifyPriceDrop(@RequestBody PriceDropNoticeParam param) {
        return JsonResp.ok(noticeService.notifyPriceDrop(param));
    }

    /** 进入秒杀通知：向收藏该商品的会员批量下发站内信 */
    @PostMapping("/internal/seckill-join")
    public JsonResp notifySeckillJoin(@RequestBody SeckillJoinNoticeParam param) {
        return JsonResp.ok(noticeService.notifySeckillJoin(param));
    }
}
