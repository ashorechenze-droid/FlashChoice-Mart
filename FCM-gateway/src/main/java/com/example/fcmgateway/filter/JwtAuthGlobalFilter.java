package com.example.fcmgateway.filter;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.util.JwtTokenService;
import com.example.fcmgateway.config.AuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证全局过滤器（网关统一校验层）
 *
 * <p>职责（详见详细设计 6.1 认证鉴权流程）：
 * <ol>
 *   <li>公开白名单/公开 GET 查询直接放行；</li>
 *   <li>其余请求校验 {@code Authorization: Bearer {accessToken}}（签名/过期/签发者/令牌用途），失败返回 401；</li>
 *   <li>Redis 黑名单检查（登出/改密/禁用后令牌已被吊销），命中返回 401；</li>
 *   <li>校验通过后注入 X-User-Id / X-User-Name / X-Audience 身份请求头再转发下游
 *       （先清除客户端伪造的同名头，防止身份冒用）。</li>
 * </ol>
 * 授权（RBAC 资源判定）由后续网关 RBAC 过滤器接入 rbac-api 完成，本过滤器只做认证。
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    /** 在路由转发过滤器之前执行 */
    private static final int ORDER = -100;

    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(JwtTokenService jwtTokenService,
                               AuthProperties authProperties,
                               ReactiveStringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // CORS 预检请求不携带令牌，直接放行（实际跨域策略由网关 CORS 配置控制）
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }
        // 1. 公开白名单 / 公开 GET 查询放行
        if (isWhitelisted(path) || isPublicGet(path, method)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Bearer Token
        String token = extractBearerToken(request);
        if (token == null) {
            return unauthorized(exchange, "未登录或令牌失效");
        }

        // 3. 验签（签名/过期/签发者任一不合法即拒绝）
        Claims claims;
        try {
            claims = jwtTokenService.parseClaims(token);
        } catch (Exception e) {
            return unauthorized(exchange, e.getMessage());
        }
        String audience = jwtTokenService.getAudience(claims);
        if (!CommonConstants.AUDIENCE_ADMIN.equals(audience)
                && !CommonConstants.AUDIENCE_MEMBER.equals(audience)) {
            return unauthorized(exchange, "令牌类型不合法");
        }
        // 仅接受 Access Token：Refresh Token 只允许用于刷新接口，禁止访问业务接口
        if (!TokenConstants.TOKEN_TYPE_ACCESS.equals(jwtTokenService.getTokenType(claims))) {
            return unauthorized(exchange, "请使用 Access Token 访问");
        }
        Long userId = jwtTokenService.getUserId(claims);
        String username = jwtTokenService.getUsername(claims);

        // 4. Redis 黑名单检查（Redis 异常降级放行，令牌依赖自身有效期兜底）
        return isBlacklisted(jwtTokenService.getJti(claims), userId)
                .flatMap(blocked -> {
                    if (blocked) {
                        return unauthorized(exchange, "令牌已失效，请重新登录");
                    }
                    // 5. 注入身份头（先移除客户端伪造的同名头）后继续转发
                    ServerHttpRequest mutated = mutateIdentityHeaders(request, userId, username, audience);
                    log.debug("网关放行 userId={}, username={}, audience={}, path={}", userId, username, audience, path);
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 白名单匹配（ant 模式，任意方法） */
    private boolean isWhitelisted(String path) {
        return authProperties.getWhitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /** 公开 GET 查询匹配：path 等于前缀或以"前缀/"开头 */
    private boolean isPublicGet(String path, HttpMethod method) {
        if (method == null || !HttpMethod.GET.equals(method)) {
            return false;
        }
        List<String> prefixes = authProperties.getPublicGetPrefixes();
        return prefixes.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /** 从 Authorization 头提取 Bearer Token，格式非法返回 null */
    private String extractBearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(TokenConstants.HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(TokenConstants.BEARER_PREFIX)) {
            return header.substring(TokenConstants.BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * 黑名单检查：单令牌（jti）与整用户两级任一命中即拒绝。
     * Redis 不可用时降级为放行（仅告警），由 Access Token 自身有效期兜底，保证网关可用性
     */
    private Mono<Boolean> isBlacklisted(String jti, Long userId) {
        return redisTemplate.hasKey(TokenConstants.REDIS_BLACKLIST_JTI_KEY + jti)
                .flatMap(jtiBlocked -> jtiBlocked
                        ? Mono.just(true)
                        : redisTemplate.hasKey(TokenConstants.REDIS_BLACKLIST_USER_KEY + userId))
                .onErrorResume(e -> {
                    log.error("网关黑名单检查异常，降级放行：{}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /** 注入身份头：先移除下游信任的同名头（防客户端伪造），再写入网关解析出的真实身份 */
    private ServerHttpRequest mutateIdentityHeaders(ServerHttpRequest request, Long userId,
                                                    String username, String audience) {
        return request.mutate().headers(headers -> {
            headers.remove(CommonConstants.HEADER_USER_ID);
            headers.remove(CommonConstants.HEADER_USER_NAME);
            headers.remove(CommonConstants.HEADER_AUDIENCE);
            headers.add(CommonConstants.HEADER_USER_ID, String.valueOf(userId));
            headers.add(CommonConstants.HEADER_USER_NAME, username);
            headers.add(CommonConstants.HEADER_AUDIENCE, audience);
        }).build();
    }

    /** 统一 401 响应：{code:401, success:false, msg, data:null}，与业务响应结构一致 */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    JsonResp.error(ResponseCode.UNAUTHORIZED, msg));
        } catch (JsonProcessingException e) {
            body = "{\"code\":401,\"success\":false,\"msg\":\"未登录或令牌失效\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
