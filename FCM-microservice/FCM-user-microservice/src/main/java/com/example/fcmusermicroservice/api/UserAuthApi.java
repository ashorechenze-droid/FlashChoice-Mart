package com.example.fcmusermicroservice.api;

import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmusermicroservice.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台用户令牌接口（与 UserApi 同前缀 api/users）：
 * refresh 为公开接口（Access Token 过期时仍需换新，网关白名单放行）
 */
@RestController
@RequestMapping("api/users")
public class UserAuthApi {

    private final AuthTokenService authTokenService;

    public UserAuthApi(AuthTokenService authTokenService) {
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
