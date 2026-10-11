package com.example.fcmgateway.filter;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmgateway.config.AuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * 网关限流过滤器（Redis 计数窗口防刷）
 *
 * <p>职责（详见详细设计 5.8 / 8.2）：
 * <ol>
 *   <li>认证入口（登录/注册/验证码/刷新，即网关白名单路径）严格限流：每 IP 每分钟 20 次；</li>
 *   <li>其余接口全局宽松限流：每 IP 每分钟 300 次；</li>
 *   <li>超限返回 429 JsonResp；Redis 不可用时降级放行（仅告警），保证网关可用性。</li>
 * </ol>
 * 本过滤器在内部接口拦截之后、JWT 认证之前执行（无效令牌的洪水请求同样计入限流）。
 */
@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    /** 在内部接口拦截(-110)之后、JWT 认证(-100)之前执行 */
    private static final int ORDER = -105;

    /** 认证入口严格限流：每 IP 每分钟 20 次 */
    private static final long AUTH_LIMIT = 20;

    /** 其余接口宽松限流：每 IP 每分钟 300 次 */
    private static final long DEFAULT_LIMIT = 300;

    /** 计数窗口（略大于 60 秒，保证窗口内计数键自然过期） */
    private static final Duration WINDOW = Duration.ofSeconds(65);

    private final AuthProperties authProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitGlobalFilter(AuthProperties authProperties,
                                 ReactiveStringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        boolean authPath = isAuthPath(path);
        long limit = authPath ? AUTH_LIMIT : DEFAULT_LIMIT;
        String key = "fcm:rate:" + (authPath ? "auth" : "default") + ":"
                + clientIp(request) + ":" + Instant.now().getEpochSecond() / 60;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> count != null && count == 1L
                        ? redisTemplate.expire(key, WINDOW).thenReturn(count)
                        : Mono.just(count == null ? 0L : count))
                .onErrorResume(e -> {
                    // Redis 异常降级放行（与黑名单检查策略一致），仅告警不阻断
                    log.error("网关限流检查异常，降级放行：{}", e.getMessage());
                    return Mono.just(-1L);
                })
                .flatMap(count -> {
                    if (count > limit) {
                        log.warn("网关限流命中 ip={}, path={}, count={}", clientIp(request), path, count);
                        return tooManyRequests(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 认证入口匹配：登录/注册/验证码/刷新等网关白名单路径 */
    private boolean isAuthPath(String path) {
        return authProperties.getWhitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /** 客户端 IP：反向代理（Nginx）场景优先取 X-Forwarded-For 首个地址 */
    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null || remote.getAddress() == null
                ? "unknown" : remote.getAddress().getHostAddress();
    }

    /** 统一 429 响应：{code:429, success:false, msg, data:null}，与业务响应结构一致 */
    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    JsonResp.error(ResponseCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试"));
        } catch (JsonProcessingException e) {
            body = "{\"code\":429,\"success\":false,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
