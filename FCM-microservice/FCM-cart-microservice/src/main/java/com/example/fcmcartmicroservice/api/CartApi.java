package com.example.fcmcartmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.CartEntity;
import com.example.fcmspi.service.CartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/carts")
public class CartApi {

    private final CartService cartService;

    public CartApi(CartService cartService) {
        this.cartService = cartService;
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

    /** 会员专属操作：后台令牌直接 403（购物车数据为会员私有） */
    private void requireMember(String audience) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限会员");
        }
    }

    /** 当前会员购物车列表（商品信息经 good-api Feign 回显，服务不可用时降级为 null） */
    @GetMapping
    public JsonResp list(@RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                         @RequestParam(required = false) Long memberId) {
        return JsonResp.ok(cartService.listByMemberId(memberId(headerMemberId, memberId)));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(cartService.getById(id));
    }

    /** 按 ID 批量查询（服务间 Feign：下单结算用；不做会员过滤，调用方自行校验归属） */
    @GetMapping("/ids")
    public JsonResp listByIds(@RequestParam List<Long> ids) {
        return JsonResp.ok(cartService.listByIds(ids));
    }

    /** 加入购物车：member_id + good_id 已存在则累加数量 */
    @PostMapping
    public JsonResp add(@RequestBody CartEntity cart,
                        @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                        @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId) {
        requireMember(audience);
        if (cart.getMemberId() == null && headerMemberId != null) {
            cart.setMemberId(memberId(headerMemberId, null));
        }
        return JsonResp.ok(cartService.add(cart));
    }

    /** 修改数量（校验归属） */
    @PutMapping
    public JsonResp updateQty(@RequestBody CartEntity cart,
                              @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        requireMember(audience);
        return JsonResp.ok(cartService.updateQty(cart));
    }

    /** 批量删除（校验归属） */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids,
                                @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId,
                                @RequestParam(required = false) Long memberId) {
        requireMember(audience);
        return JsonResp.ok(cartService.deleteByIds(memberId(headerMemberId, memberId), ids));
    }
}
