package com.example.fcmcommon.config;

import com.example.fcmcommon.constant.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（fcm.jwt.*）
 * 关键约束：签发方（user-api / member-api）与校验方（网关）必须使用同一 secret，
 * 建议统一维护到 Nacos 配置中心（本地 application.yaml 已给出同名默认值）
 */
@Data
@ConfigurationProperties(prefix = "fcm.jwt")
public class JwtProperties {

    /** HS256 签名密钥（安全要求 ≥ 32 字节，多服务保持一致） */
    private String secret;

    /** 签发者（解析时强校验） */
    private String issuer = "flashchoice-mart";

    /** 后台用户（admin）有效期 */
    private Ttl admin = new Ttl(720, 10080);

    /** 会员（member）有效期 */
    private Ttl member = new Ttl(10080, 43200);

    /** 按 audience 取 Access Token 有效期（分钟） */
    public long accessTtlMinutes(String audience) {
        return ttlOf(audience).getAccessMinutes();
    }

    /** 按 audience 取 Refresh Token 有效期（分钟） */
    public long refreshTtlMinutes(String audience) {
        return ttlOf(audience).getRefreshMinutes();
    }

    private Ttl ttlOf(String audience) {
        return CommonConstants.AUDIENCE_MEMBER.equals(audience) ? member : admin;
    }

    /**
     * 令牌有效期（分钟）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ttl {

        /** Access Token 有效期（分钟） */
        private long accessMinutes = 720;

        /** Refresh Token 有效期（分钟） */
        private long refreshMinutes = 10080;
    }
}
