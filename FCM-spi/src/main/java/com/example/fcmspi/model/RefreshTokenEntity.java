package com.example.fcmspi.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌（t_refresh_token）：JWT 双 Token 认证支撑表实体
 * 由 user-api（audience=admin）与 member-api（audience=member）共用；
 * 一次性轮换：刷新成功即删除旧记录并写入新记录，登出时按用户删除实现主动失效
 */
@Data
public class RefreshTokenEntity {

    /** 主键 */
    private Long id;

    /** 令牌类型（admin 后台用户 / member 会员，与签发的 JWT audience 一致） */
    private String audience;

    /** 用户 ID（按 audience 关联 user / member 主键） */
    private Long userId;

    /** 刷新令牌（JWT 串） */
    private String token;

    /** 过期时间（与令牌内 exp 一致） */
    private LocalDateTime expireTime;

    /** 创建时间（DB 默认 CURRENT_TIMESTAMP） */
    private LocalDateTime createTime;
}
