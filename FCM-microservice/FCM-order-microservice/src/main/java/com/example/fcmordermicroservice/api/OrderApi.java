package com.example.fcmordermicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.OrderCreateParam;
import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.SeckillOrderParam;
import com.example.fcmspi.model.search.OrderSearchBean;
import com.example.fcmspi.service.OrderService;
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
import java.util.Map;

@RestController
@RequestMapping("api/orders")
public class OrderApi {

    private final OrderService orderService;

    public OrderApi(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 订单分页：member 令牌强制按本人过滤；admin 令牌不做会员过滤（可传 memberAccount 参数筛选）；
     * 直连调试（无 X-Audience 头）时以 X-User-Name 身份头为准
     */
    @GetMapping
    public JsonResp page(OrderSearchBean search,
                         @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                         @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        if (!CommonConstants.AUDIENCE_ADMIN.equals(audience) && headerAccount != null) {
            search.setMemberAccount(headerAccount);
        }
        return JsonResp.ok(orderService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    /** 订单详情（默认含明细；会员仅可查看本人订单） */
    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id,
                            @RequestParam(defaultValue = "true") boolean withItems,
                            @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                            @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount) {
        return JsonResp.ok(checkReadable(orderService.getById(id, withItems), audience, headerAccount));
    }

    /** 按订单号查询（含明细；秒杀抢购结果轮询/服务间 Feign 使用，会员仅可查看本人订单） */
    @GetMapping("/no/{orderNo}")
    public JsonResp getByOrderNo(@PathVariable String orderNo,
                                 @RequestParam(defaultValue = "true") boolean withItems,
                                 @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                 @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount) {
        return JsonResp.ok(checkReadable(orderService.getByOrderNo(orderNo, withItems), audience, headerAccount));
    }

    /** 提交订单：{cartIds[] 或 goodId+qty, addrId, comment}，memberAccount/memberId 由网关身份头注入 */
    @PostMapping
    public JsonResp createOrder(@RequestBody OrderCreateParam param,
                                @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                                @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerMemberId) {
        requireMember(audience);
        if (param.getMemberAccount() == null && headerAccount != null) {
            param.setMemberAccount(headerAccount);
        }
        if (param.getMemberId() == null && headerMemberId != null) {
            try {
                param.setMemberId(Long.valueOf(headerMemberId));
            } catch (NumberFormatException ignored) {
                // 头非法时忽略，购物车结算会因缺少会员身份被拒绝
            }
        }
        return JsonResp.ok(orderService.createOrder(param));
    }

    /** Service-to-service endpoint; the gateway blocks external calls. */
    @PostMapping("/internal/seckill")
    public JsonResp createSeckillOrder(@RequestBody SeckillOrderParam param) {
        return JsonResp.ok(orderService.createSeckillOrder(param));
    }

    /** 发起支付：返回模拟收银台信息（会员本人） */
    @PutMapping("/{id}/pay")
    public JsonResp pay(@PathVariable Long id,
                        @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                        @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                        @RequestParam(required = false) String account) {
        String memberAccount = requireMemberAccount(audience, headerAccount, account);
        OrderEntity order = orderService.getById(id, false);
        if (order == null) {
            return JsonResp.error(ResponseCode.NOT_FOUND, "订单不存在");
        }
        if (!memberAccount.equals(order.getMemberAccount())) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权操作他人订单");
        }
        Map<String, Object> cashier = Map.of(
                "orderNo", order.getOrderNo(),
                "totalPay", order.getTotalPay(),
                "payType", "模拟支付",
                "message", "模拟收银台：确认后将订单置为已支付");
        return JsonResp.ok(cashier);
    }

    /** 模拟支付确认：{payType:"模拟支付"} → 已支付（会员本人） */
    @PostMapping("/{id}/pay/confirm")
    public JsonResp payConfirm(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body,
                               @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                               @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                               @RequestParam(required = false) String account) {
        String memberAccount = requireMemberAccount(audience, headerAccount, account);
        String payType = body != null && body.get("payType") != null ? body.get("payType") : "模拟支付";
        orderService.payConfirm(id, memberAccount, payType);
        return JsonResp.ok("支付成功", null);
    }

    /** 取消订单（待付款）：会员校验本人，后台（admin）跳过归属校验 */
    @PutMapping("/{id}/cancel")
    public JsonResp cancel(@PathVariable Long id,
                           @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                           @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                           @RequestParam(required = false) String account) {
        return JsonResp.ok(orderService.cancel(id, ownershipAccount(audience, headerAccount, account)));
    }

    /** 后台发货（已支付 → 待收货），仅限后台令牌 */
    @PutMapping("/{id}/ship")
    public JsonResp ship(@PathVariable Long id,
                         @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        requireAdmin(audience);
        return JsonResp.ok(orderService.ship(id));
    }

    /** 会员确认收货（待收货 → 已确认，会员本人） */
    @PutMapping("/{id}/confirm")
    public JsonResp confirmReceive(@PathVariable Long id,
                                   @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                   @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                                   @RequestParam(required = false) String account) {
        return JsonResp.ok(orderService.confirmReceive(id, requireMemberAccount(audience, headerAccount, account)));
    }

    /** 发起退款（无退款 → 退款中，模拟退款）：会员校验本人，后台跳过归属校验 */
    @PutMapping("/{id}/refund")
    public JsonResp applyRefund(@PathVariable Long id,
                                @RequestBody(required = false) Map<String, String> body,
                                @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                                @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                                @RequestParam(required = false) String account) {
        String reason = body != null ? body.get("reason") : null;
        return JsonResp.ok(orderService.applyRefund(id, reason, ownershipAccount(audience, headerAccount, account)));
    }

    /** 后台确认退款（退款中 → 已退款），仅限后台令牌 */
    @PutMapping("/{id}/refund/confirm")
    public JsonResp confirmRefund(@PathVariable Long id,
                                  @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        requireAdmin(audience);
        return JsonResp.ok(orderService.confirmRefund(id));
    }

    /** 关闭超时订单（服务间内部接口：RabbitMQ 延时消息消费触发；定时扫描兜底） */
    @PutMapping("/close-timeout/{orderNo}")
    public JsonResp closeTimeoutOrder(@PathVariable String orderNo) {
        return JsonResp.ok(orderService.closeTimeoutOrder(orderNo));
    }

    /** 后台逻辑删除（仅限后台令牌） */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids,
                                @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        requireAdmin(audience);
        return JsonResp.ok(orderService.deleteByIds(ids));
    }

    /* ==================== 身份判定辅助（X-Audience：member/admin；直连调试时为空） ==================== */

    /** 会员专属操作：后台令牌直接 403 */
    private void requireMember(String audience) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限会员");
        }
    }

    /** 会员专属操作并解析会员账号（身份头优先，直连调试用查询参数兜底），后台令牌 403 */
    private String requireMemberAccount(String audience, String headerAccount, String paramAccount) {
        requireMember(audience);
        String account = headerAccount != null ? headerAccount : paramAccount;
        if (account == null || account.isBlank()) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "缺少会员身份，请先登录");
        }
        return account;
    }

    /** 后台专属操作：会员令牌直接 403（admin / 直连调试放行） */
    private void requireAdmin(String audience) {
        if (CommonConstants.AUDIENCE_MEMBER.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限后台");
        }
    }

    /** 归属校验账号：admin 令牌返回 null（跳过归属校验），其余返回身份账号（可为空，由业务层兜底） */
    private String ownershipAccount(String audience, String headerAccount, String paramAccount) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            return null;
        }
        return headerAccount != null ? headerAccount : paramAccount;
    }

    /** 会员令牌读取订单时校验归属（admin 与直连调试不校验） */
    private OrderEntity checkReadable(OrderEntity order, String audience, String headerAccount) {
        if (order != null && CommonConstants.AUDIENCE_MEMBER.equals(audience) && headerAccount != null
                && !headerAccount.equals(order.getMemberAccount())) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权查看他人订单");
        }
        return order;
    }
}
