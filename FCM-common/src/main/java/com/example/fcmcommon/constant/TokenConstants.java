package com.example.fcmcommon.constant;

/**
 * JWT 令牌常量：认证请求头、Claims 名、Redis 黑名单 Key
 */
public final class TokenConstants {

    private TokenConstants() {
    }

    /** 认证请求头（标准 Authorization） */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    /** Bearer 前缀（含尾随空格） */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Claim：用户 ID（Long） */
    public static final String CLAIM_USER_ID = "userId";
    /** Claim：用户账号（后台 username / 会员 account，String） */
    public static final String CLAIM_USERNAME = "username";
    /** Claim：令牌类型（admin / member，与 CommonConstants.AUDIENCE_* 对应） */
    public static final String CLAIM_AUDIENCE = "audience";
    /** Claim：令牌用途（access / refresh，防止 Refresh Token 被当作 Access Token 使用） */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    /** Claim：JWT ID（登出/强制下线按 jti 精确吊销） */
    public static final String CLAIM_JTI = "jti";

    /** 令牌用途：访问令牌（逐请求携带，网关注入身份） */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** 令牌用途：刷新令牌（仅用于 /refresh 换新，禁止访问业务接口） */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** Redis 黑名单 Key 前缀 */
    public static final String REDIS_BLACKLIST_PREFIX = "fcm:auth:blacklist:";
    /** 单 Token 黑名单 Key 前缀（完整 Key：{prefix}jti:{jti}） */
    public static final String REDIS_BLACKLIST_JTI_KEY = REDIS_BLACKLIST_PREFIX + "jti:";
    /** 整用户黑名单 Key 前缀（完整 Key：{prefix}user:{userId}，强制该用户所有 Access Token 失效） */
    public static final String REDIS_BLACKLIST_USER_KEY = REDIS_BLACKLIST_PREFIX + "user:";
}
