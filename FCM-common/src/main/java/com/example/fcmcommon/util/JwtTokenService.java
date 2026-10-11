package com.example.fcmcommon.util;

import com.example.fcmcommon.config.JwtProperties;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.dto.TokenPair;
import com.example.fcmcommon.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌服务：签发 / 解析（HS256 对称签名，签发方与校验方共用同一 secret）
 *
 * <p>双 Token 机制约定：
 * <ul>
 *   <li>Access Token：短效，随 {@code Authorization: Bearer xxx} 逐请求携带，由网关校验；</li>
 *   <li>Refresh Token：长效一次性，仅用于刷新接口换新（服务端落库轮换，防重放）。</li>
 * </ul>
 * Claims：sub=userId，userId / username（后台 username、会员 account）/ audience / tokenType（access|refresh）/ jti
 */
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("fcm.jwt.secret 未配置或不足 32 字节（HS256 安全要求）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发双 Token（Access + Refresh 同 Claims，仅有效期与 tokenType 不同）
     *
     * @param userId   用户 ID
     * @param username 用户账号（后台 username / 会员 account，注入网关 X-User-Name）
     * @param audience 令牌类型（CommonConstants.AUDIENCE_ADMIN / AUDIENCE_MEMBER）
     */
    public TokenPair generateTokenPair(Long userId, String username, String audience) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        String accessToken = createToken(userId, username, audience, TokenConstants.TOKEN_TYPE_ACCESS, now,
                properties.accessTtlMinutes(audience) * 60_000L);
        String refreshToken = createToken(userId, username, audience, TokenConstants.TOKEN_TYPE_REFRESH, now,
                properties.refreshTtlMinutes(audience) * 60_000L);
        return new TokenPair(accessToken, refreshToken,
                properties.accessTtlMinutes(audience) * 60L);
    }

    /**
     * 解析并校验 Token（签名、过期、签发者），任一不合法抛 BizException(401)
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            throw new BizException(ResponseCode.UNAUTHORIZED, "令牌无效或已过期");
        }
    }

    public Long getUserId(Claims claims) {
        return claims.get(TokenConstants.CLAIM_USER_ID, Long.class);
    }

    public String getUsername(Claims claims) {
        return claims.get(TokenConstants.CLAIM_USERNAME, String.class);
    }

    public String getAudience(Claims claims) {
        return claims.get(TokenConstants.CLAIM_AUDIENCE, String.class);
    }

    public String getTokenType(Claims claims) {
        return claims.get(TokenConstants.CLAIM_TOKEN_TYPE, String.class);
    }

    public String getJti(Claims claims) {
        return claims.get(TokenConstants.CLAIM_JTI, String.class);
    }

    /** 令牌剩余有效秒数（登出吊销黑名单 TTL 用，最短 1 秒） */
    public long getRemainingSeconds(Claims claims) {
        if (claims.getExpiration() == null) {
            return 1;
        }
        return Math.max((claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000, 1);
    }

    private String createToken(Long userId, String username, String audience, String tokenType,
                               Date issuedAt, long ttlMillis) {
        Date expiresAt = new Date(issuedAt.getTime() + ttlMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TokenConstants.CLAIM_USER_ID, userId)
                .claim(TokenConstants.CLAIM_USERNAME, username)
                .claim(TokenConstants.CLAIM_AUDIENCE, audience)
                .claim(TokenConstants.CLAIM_TOKEN_TYPE, tokenType)
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }
}
