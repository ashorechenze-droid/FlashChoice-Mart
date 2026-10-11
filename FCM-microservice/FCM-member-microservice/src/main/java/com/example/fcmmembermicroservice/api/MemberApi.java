package com.example.fcmmembermicroservice.api;

import com.example.fcmcommon.dto.TokenPair;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.service.CaptchaService;
import com.example.fcmmembermicroservice.dto.LoginResult;
import com.example.fcmmembermicroservice.dto.MemberRegisterParam;
import com.example.fcmmembermicroservice.service.AuthTokenService;
import com.example.fcmspi.model.MemberEntity;
import com.example.fcmspi.model.search.MemberSearchBean;
import com.example.fcmspi.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/members")
public class MemberApi {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final CaptchaService captchaService;

    public MemberApi(MemberService memberService, AuthTokenService authTokenService,
                     CaptchaService captchaService) {
        this.memberService = memberService;
        this.authTokenService = authTokenService;
        this.captchaService = captchaService;
    }

    /** 获取图形验证码（公开）：返回 `{captchaId, imageBase64}`，登录/注册时回传校验（2 分钟有效一次性） */
    @GetMapping("/captcha")
    public JsonResp captcha() {
        return JsonResp.ok(captchaService.generate());
    }

    /** 会员分页（后台，关键字：账号/姓名/手机号） */
    @GetMapping
    public JsonResp page(MemberSearchBean search,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        return JsonResp.ok(memberService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(memberService.getById(id));
    }

    /** 会员注册（公开）：先校验图形验证码，再账号唯一校验 + BCrypt 加密 */
    @PostMapping("/register")
    public JsonResp register(@RequestBody MemberRegisterParam param) {
        captchaService.validate(param.getCaptchaId(), param.getCaptcha());
        return JsonResp.ok(memberService.register(param));
    }

    /** 会员登录（公开）：先校验图形验证码，再校验密码后签发双 Token（JWT） */
    @PostMapping("/login")
    public JsonResp login(@RequestBody Map<String, String> body) {
        captchaService.validate(body.get("captchaId"), body.get("captcha"));
        String account = body.get("account");
        String password = body.get("password");
        MemberEntity member = memberService.login(account, password);
        TokenPair pair = authTokenService.issue(member);
        return JsonResp.ok(new LoginResult(member, pair.getAccessToken(), pair.getRefreshToken(),
                pair.getTokenType(), pair.getExpiresIn()));
    }

    /** 资料更新（乐观锁 version 校验） */
    @PutMapping
    public JsonResp update(@RequestBody MemberEntity member) {
        return JsonResp.ok(memberService.update(member));
    }

    /** 修改密码（需校验原密码） */
    @PutMapping("/{id}/password")
    public JsonResp updatePassword(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return JsonResp.ok(memberService.updatePassword(id, body.get("oldPassword"), body.get("newPassword")));
    }

    /** 启用/禁用（后台） */
    @PutMapping("/{id}/enabled")
    public JsonResp updateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return JsonResp.ok(memberService.updateEnabled(id, enabled));
    }

    /** 重置密码（后台） */
    @PutMapping("/{id}/reset-password")
    public JsonResp resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        return JsonResp.ok(memberService.resetPassword(id, newPassword));
    }
}
