package com.example.fcmmemberaddressmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.MemberAddressEntity;
import com.example.fcmspi.service.MemberAddressService;
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

@RestController
@RequestMapping("api/member-addresses")
public class MemberAddressApi {

    private final MemberAddressService memberAddressService;

    public MemberAddressApi(MemberAddressService memberAddressService) {
        this.memberAddressService = memberAddressService;
    }

    /**
     * 地址列表：管理端令牌按 account 参数查询指定会员（会员详情弹窗）；
     * 会员令牌强制查询本人（网关注入身份头不可伪造，account 参数仅直连调试兜底）
     */
    @GetMapping
    public JsonResp list(@RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                         @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                         @RequestParam(required = false) String account) {
        String memberAccount = resolveMemberAccount(audience, headerAccount, account);
        return JsonResp.ok(memberAddressService.listByMemberAccount(memberAccount));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(memberAddressService.getById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody MemberAddressEntity address,
                           @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                           @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount) {
        requireMember(audience);
        // 会员令牌强制挂到本人名下（防伪造 memberAccount）；直连调试保留请求体值
        if (headerAccount != null) {
            address.setMemberAccount(headerAccount);
        }
        return JsonResp.ok(memberAddressService.insert(address));
    }

    @PutMapping
    public JsonResp update(@RequestBody MemberAddressEntity address,
                           @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                           @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount) {
        requireMember(audience);
        // 会员令牌以本人身份为准：置默认时清除同会员其他默认地址依赖该字段（前端请求体不含）
        if (headerAccount != null) {
            address.setMemberAccount(headerAccount);
        }
        return JsonResp.ok(memberAddressService.update(address));
    }

    @DeleteMapping("/{id}")
    public JsonResp deleteById(@PathVariable Long id,
                               @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience) {
        requireMember(audience);
        return JsonResp.ok(memberAddressService.deleteById(id));
    }

    /** 设为默认地址（同一会员仅一条 is_default=1） */
    @PutMapping("/{id}/default")
    public JsonResp setDefault(@PathVariable Long id,
                               @RequestHeader(value = CommonConstants.HEADER_AUDIENCE, required = false) String audience,
                               @RequestHeader(value = CommonConstants.HEADER_USER_NAME, required = false) String headerAccount,
                               @RequestParam(required = false) String account) {
        requireMember(audience);
        String memberAccount = headerAccount != null ? headerAccount : account;
        return JsonResp.ok(memberAddressService.setDefault(id, memberAccount));
    }

    /* ==================== 身份解析（X-Audience：admin/member；直连调试时为空） ==================== */

    /**
     * 解析目标会员账号：
     * 管理端令牌（X-Audience=admin）以显式 account 参数为准，可查询任意会员（地址列表只读场景）；
     * 会员令牌/直连调试以网关注入的身份头优先（网关已剥离伪造头），account 参数仅作兜底
     */
    private String resolveMemberAccount(String audience, String headerAccount, String paramAccount) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            return paramAccount;
        }
        return headerAccount != null ? headerAccount : paramAccount;
    }

    /** 会员专属写操作：后台令牌直接 403（地址数据为会员私有，管理端仅支持列表查询） */
    private void requireMember(String audience) {
        if (CommonConstants.AUDIENCE_ADMIN.equals(audience)) {
            throw new BizException(ResponseCode.FORBIDDEN, "该操作仅限会员");
        }
    }
}
