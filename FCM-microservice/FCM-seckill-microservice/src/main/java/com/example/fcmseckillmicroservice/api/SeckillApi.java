package com.example.fcmseckillmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmspi.model.SeckillEntity;
import com.example.fcmspi.model.SeckillGoodEntity;
import com.example.fcmspi.model.search.SeckillSearchBean;
import com.example.fcmspi.service.SeckillService;
import com.example.fcmseckillmicroservice.service.impl.SeckillServiceImpl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

/**
 * 秒杀中心：活动、商品和异步抢购。
 */
@RestController
@RequestMapping("api/seckills")
public class SeckillApi {

    private final SeckillService seckillService;
    private final SeckillServiceImpl implementation;

    public SeckillApi(SeckillService seckillService, SeckillServiceImpl implementation) {
        this.seckillService = seckillService;
        this.implementation = implementation;
    }

    /* ==================== 秒杀活动 ==================== */

    /** 活动分页（后台/通用）：keyword 名称模糊、enabled 状态过滤 */
    @GetMapping
    public JsonResp page(SeckillSearchBean search,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        return JsonResp.ok(seckillService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    /** 前台活动列表（公开）：启用且未结束，进行中在前；无需分页 */
    @GetMapping("/active")
    public JsonResp listActive() {
        return JsonResp.ok(seckillService.listActive());
    }

    /** 活动详情（withGoods=true 时携带活动商品列表，供秒杀会场使用） */
    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id,
                            @RequestParam(defaultValue = "true") boolean withGoods) {
        SeckillEntity seckill = seckillService.getById(id);
        if (seckill != null && withGoods) {
            seckill.setSeckillGoods(seckillService.listGoods(id));
        }
        return JsonResp.ok(seckill);
    }

    /** 新增活动（后台）：{name, enabled?, startTime, endTime, description?} */
    @PostMapping
    public JsonResp insert(@RequestBody SeckillEntity seckill) {
        return JsonResp.ok(seckillService.insert(seckill));
    }

    /** 修改活动（后台）：{id, name?, enabled?, startTime?, endTime?, description?} */
    @PutMapping
    public JsonResp update(@RequestBody SeckillEntity seckill) {
        return JsonResp.ok(seckillService.update(seckill));
    }

    /** 启用/禁用活动（后台）：PUT /api/seckills/{id}/enabled?enabled=true */
    @PutMapping("/{id}/enabled")
    public JsonResp updateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return JsonResp.ok(seckillService.updateEnabled(id, enabled));
    }

    /** 删除活动（后台）：级联删除活动商品 */
    @DeleteMapping("/{id}")
    public JsonResp deleteById(@PathVariable Long id) {
        return JsonResp.ok(seckillService.deleteById(id));
    }

    /* ==================== 秒杀活动商品 ==================== */

    /** 活动商品列表（公开）：含秒杀价/库存/已售/限购，剩余库存 = stock - sold */
    @GetMapping("/{seckillId}/goods")
    public JsonResp listGoods(@PathVariable Long seckillId) {
        return JsonResp.ok(seckillService.listGoods(seckillId));
    }

    /** 单个秒杀商品详情（公开）：下单/详情页使用 */
    @GetMapping("/goods/{id}")
    public JsonResp getGoodById(@PathVariable Long id) {
        return JsonResp.ok(seckillService.getGoodById(id));
    }

    /** 活动新增商品（后台）：{goodId, seckillPrice, stock, sold?, limitPerUser?, description?}，seckillId 取路径 */
    @PostMapping("/{seckillId}/goods")
    public JsonResp insertGood(@PathVariable Long seckillId, @RequestBody SeckillGoodEntity seckillGood) {
        seckillGood.setSeckillId(seckillId);
        return JsonResp.ok(seckillService.insertGood(seckillGood));
    }

    /** 修改秒杀商品（后台）：{id, seckillPrice?, stock?, sold?, limitPerUser?, description?}（关联关系不可变更） */
    @PutMapping("/goods")
    public JsonResp updateGood(@RequestBody SeckillGoodEntity seckillGood) {
        return JsonResp.ok(seckillService.updateGood(seckillGood));
    }

    /** 删除秒杀商品（后台） */
    @DeleteMapping("/goods/{id}")
    public JsonResp deleteGood(@PathVariable Long id) {
        return JsonResp.ok(seckillService.deleteGood(id));
    }

    @PostMapping("/{id}/warm-up")
    public JsonResp warmUp(@PathVariable Long id,
                           @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        if (!CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "仅管理员可预热库存");
        }
        seckillService.warmUp(id);
        return JsonResp.ok(1);
    }

    @PostMapping("/grab")
    public JsonResp grab(@RequestBody Map<String, Long> body,
                         @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                         @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long memberId,
                         @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String account) {
        requireMember(audience, memberId, account);
        if (body == null) throw new BizException(ResponseCode.BAD_REQUEST, "缺少抢购参数");
        String seckillNo = seckillService.grab(body.get("seckillGoodId"), memberId, account, body.get("addrId"));
        return JsonResp.ok(Map.of("orderNo", seckillNo, "status", "处理中"));
    }

    @GetMapping("/grab/{orderNo}")
    public JsonResp grabResult(@PathVariable String orderNo,
                               @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                               @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long memberId,
                               @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String account) {
        requireMember(audience, memberId, account);
        return JsonResp.ok(seckillService.getGrabResult(orderNo, memberId));
    }

    @PutMapping("/internal/release/{seckillNo}")
    public JsonResp release(@PathVariable String seckillNo) {
        return JsonResp.ok(implementation.releaseGrab(seckillNo));
    }

    private void requireMember(String audience, Long memberId, String account) {
        if (!CommonConstants.AUDIENCE_MEMBER.equals(audience) || memberId == null || account == null || account.isBlank()) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "请先以会员身份登录");
        }
    }
}
