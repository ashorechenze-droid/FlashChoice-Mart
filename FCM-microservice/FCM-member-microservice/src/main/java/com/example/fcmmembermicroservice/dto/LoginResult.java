package com.example.fcmmembermicroservice.dto;

import com.example.fcmspi.model.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 会员登录响应：会员信息 + 双 Token
 * 原登录响应 data 为 MemberEntity，接入 JWT 后 data 结构调整为 {member, accessToken, refreshToken, ...}
 */
@Data
@AllArgsConstructor
public class LoginResult {

    /** 会员信息（密码不回传） */
    private MemberEntity member;

    /** Access Token（短效，请求头 Authorization: Bearer 携带） */
    private String accessToken;

    /** Refresh Token（长效一次性，用于刷新换新） */
    private String refreshToken;

    /** 令牌类型（固定 Bearer） */
    private String tokenType = "Bearer";

    /** Access Token 剩余有效秒数 */
    private Long expiresIn;
}
