package com.example.fcmmembermicroservice.api;

import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmmembermicroservice.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 会员令牌接口（与 MemberApi 同前缀 api/members）：
 * refresh 为公开接口（Access Token 过期时仍需换新，网关白名单放行）
 */
@RestController
@RequestMapping("api/members")
public class MemberAuthApi {

    private final AuthTokenService authTokenService;

    public MemberAuthApi(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    /** 刷新双 Token（公开）：Refresh Token 一次性使用，成功即返回新的一对令牌 */
    @PostMapping("/refresh")
    public JsonResp refresh(@RequestBody Map<String, String> body) {
        return JsonResp.ok(authTokenService.refresh(body.get("refreshToken")));
    }

    /**
     * 登出（登录）：吊销当前 Access Token（Redis 黑名单），并删除 Refresh Token 记录
     * 请求头需携带 Authorization: Bearer {accessToken}
     */
    @PostMapping("/logout")
    public JsonResp logout(HttpServletRequest request,
                           @RequestBody(required = false) Map<String, String> body) {
        String refreshToken = body == null ? null : body.get("refreshToken");
        authTokenService.logout(request.getHeader(TokenConstants.HEADER_AUTHORIZATION), refreshToken);
        return JsonResp.ok();
    }
}
