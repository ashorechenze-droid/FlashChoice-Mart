package com.example.fcmmembermicroservice.service;

import com.example.fcmcommon.config.JwtProperties;
import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmmembermicroservice.mapper.RefreshTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Access Token 黑名单服务（Redis）——令牌主动失效机制
 *
 * <ul>
 *   <li>单令牌吊销（登出）：key = fcm:auth:blacklist:jti:{jti}，TTL = 该 Access Token 剩余有效期；</li>
 *   <li>整用户吊销（改密/重置密码/禁用）：key = fcm:auth:blacklist:user:{userId}，
 *       TTL = Refresh Token 有效期，网关命中即拒绝该用户所有 Access Token。</li>
 * </ul>
 * 降级策略：Redis 短暂不可用时仅记录告警、不阻断主流程——令牌将依赖自身有效期自然失效
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtProperties jwtProperties;

    public TokenBlacklistService(StringRedisTemplate redisTemplate,
                                 RefreshTokenMapper refreshTokenMapper,
                                 JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenMapper = refreshTokenMapper;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 登出吊销：当前 Access Token 的 jti 进黑名单，并删除该设备对应的 Refresh Token 记录
     *
     * @param audience     令牌类型（admin / member）
     * @param userId       用户 ID（仅日志）
     * @param jti          当前 Access Token 的 jti
     * @param ttlSeconds   黑名单保留秒数（取 Access Token 剩余有效期）
     * @param refreshToken 待删除的 Refresh Token（可空，空则仅吊销 Access Token）
     */
    public void revokeCurrent(String audience, Long userId, String jti, long ttlSeconds, String refreshToken) {
        // 1) 删除 Refresh Token 记录（登出即失效，杜绝旧 Refresh Token 换新）
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenMapper.deleteByToken(audience, refreshToken);
        }
        // 2) Access Token jti 写入黑名单
        try {
            redisTemplate.opsForValue()
                    .set(TokenConstants.REDIS_BLACKLIST_JTI_KEY + jti, "1", ttlSeconds, TimeUnit.SECONDS);
            log.info("会员 {} 登出成功，Access Token 已吊销（{}s）", userId, ttlSeconds);
        } catch (Exception e) {
            log.error("Access Token 吊销写入 Redis 失败：{}（该令牌将依赖自身有效期失效）", e.getMessage());
        }
    }

    /**
     * 强制下线：删除该用户全部 Refresh Token 记录，并置位用户级黑名单（其所有 Access Token 立即失效）
     */
    public void revokeAllUserTokens(String audience, Long userId) {
        refreshTokenMapper.deleteByUserId(audience, userId);
        try {
            long ttlMinutes = jwtProperties.refreshTtlMinutes(audience);
            redisTemplate.opsForValue()
                    .set(TokenConstants.REDIS_BLACKLIST_USER_KEY + userId, "1", ttlMinutes, TimeUnit.MINUTES);
            log.info("会员 {} 全部令牌已吊销（黑名单 {} 分钟）", userId, ttlMinutes);
        } catch (Exception e) {
            log.error("会员 {} 全量令牌吊销写入 Redis 失败：{}（其令牌将依赖自身有效期失效）", userId, e.getMessage());
        }
    }
}
