package com.example.fcmmembermicroservice.service;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.dto.TokenPair;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.util.JwtTokenService;
import com.example.fcmmembermicroservice.mapper.MemberMapper;
import com.example.fcmmembermicroservice.mapper.RefreshTokenMapper;
import com.example.fcmspi.model.MemberEntity;
import com.example.fcmspi.model.RefreshTokenEntity;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 会员令牌服务（audience=member）：双 Token 签发 / 刷新 / 登出
 *
 * <ul>
 *   <li>登录签发：Access + Refresh 一对，Refresh 落库（t_refresh_token，一次性轮换）；</li>
 *   <li>刷新：校验旧 Refresh Token（存在、未过期、账号仍启用）后删旧存新，防重放；</li>
 *   <li>登出：当前 Access Token 的 jti 进 Redis 黑名单，同时删除 Refresh 记录。</li>
 * </ul>
 */
@Service
public class AuthTokenService {

    /** 本服务签发的令牌类型（会员） */
    private static final String AUDIENCE = CommonConstants.AUDIENCE_MEMBER;

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenMapper refreshTokenMapper;
    private final MemberMapper memberMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthTokenService(JwtTokenService jwtTokenService,
                            RefreshTokenMapper refreshTokenMapper,
                            MemberMapper memberMapper,
                            TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenMapper = refreshTokenMapper;
        this.memberMapper = memberMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /** 登录成功后签发双 Token（Refresh Token 落库），并顺带清理本端过期记录 */
    public TokenPair issue(MemberEntity member) {
        refreshTokenMapper.deleteExpired();
        TokenPair pair = jwtTokenService.generateTokenPair(member.getId(), member.getAccount(), AUDIENCE);
        saveRefreshToken(member.getId(), pair.getRefreshToken());
        return pair;
    }

    /**
     * 刷新双 Token：旧 Refresh Token 一次性使用（删旧存新）
     * 刷新时复核账号状态（启用），被禁用会员无法续期
     */
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "刷新令牌不能为空");
        }
        RefreshTokenEntity record = refreshTokenMapper.findByToken(AUDIENCE, refreshToken);
        if (record == null || record.getExpireTime() == null
                || record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "刷新令牌无效或已过期，请重新登录");
        }
        MemberEntity member = memberMapper.findById(record.getUserId());
        if (member == null || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        // 一次性轮换：删旧 → 签新 → 存新
        refreshTokenMapper.deleteByToken(AUDIENCE, refreshToken);
        TokenPair pair = jwtTokenService.generateTokenPair(member.getId(), member.getAccount(), AUDIENCE);
        saveRefreshToken(member.getId(), pair.getRefreshToken());
        return pair;
    }

    /**
     * 登出：吊销请求携带的 Access Token（jti 黑名单），并删除对应 Refresh Token 记录
     *
     * @param authorizationHeader 请求头 Authorization（Bearer 开头）
     * @param refreshToken        前端持有的 Refresh Token（可空）
     */
    public void logout(String authorizationHeader, String refreshToken) {
        Claims claims = parseBearerClaims(authorizationHeader);
        Long userId = jwtTokenService.getUserId(claims);
        tokenBlacklistService.revokeCurrent(AUDIENCE, userId,
                jwtTokenService.getJti(claims), jwtTokenService.getRemainingSeconds(claims), refreshToken);
    }

    /**
     * 从 Authorization 头解析并校验 Access Token（签名/过期/签发者），
     * 并确认令牌 audience 为本端（会员）、用途为 Access，防止跨端/跨用途令牌调用
     */
    private Claims parseBearerClaims(String authorizationHeader) {
        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith(TokenConstants.BEARER_PREFIX)) {
            token = authorizationHeader.substring(TokenConstants.BEARER_PREFIX.length());
        }
        if (token == null || token.isBlank()) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "未携带令牌");
        }
        Claims claims = jwtTokenService.parseClaims(token);
        if (!AUDIENCE.equals(jwtTokenService.getAudience(claims))) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "令牌类型不匹配");
        }
        if (!TokenConstants.TOKEN_TYPE_ACCESS.equals(jwtTokenService.getTokenType(claims))) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "请使用 Access Token 操作");
        }
        return claims;
    }

    /** Refresh Token 落库：过期时间取令牌内 exp，保证与 JWT 一致 */
    private void saveRefreshToken(Long userId, String refreshToken) {
        Claims claims = jwtTokenService.parseClaims(refreshToken);
        Date expiresAt = claims.getExpiration();
        RefreshTokenEntity record = new RefreshTokenEntity();
        record.setAudience(AUDIENCE);
        record.setUserId(userId);
        record.setToken(refreshToken);
        record.setExpireTime(LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault()));
        refreshTokenMapper.insert(record);
    }
}
