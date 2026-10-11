package com.example.fcmcommon.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 双 Token 结果（登录 / 刷新接口响应体）
 */
@Data
@NoArgsConstructor
public class TokenPair {

    /** Access Token（短效，网关逐请求校验） */
    private String accessToken;

    /** Refresh Token（长效一次性，刷新换新） */
    private String refreshToken;

    /** 令牌类型（固定 Bearer） */
    private String tokenType = "Bearer";

    /** Access Token 剩余有效秒数 */
    private Long expiresIn;

    public TokenPair(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
