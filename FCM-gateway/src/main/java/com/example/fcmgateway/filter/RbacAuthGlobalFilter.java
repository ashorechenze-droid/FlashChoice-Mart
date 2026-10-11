package com.example.fcmgateway.filter;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.response.JsonResp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台接口 RBAC 授权过滤器（网关授权层）
 *
 * <p>职责（详见详细设计 6.1 认证鉴权流程）：
 * <ol>
 *   <li>仅对「后台令牌（X-Audience=admin）+ 写操作（POST/PUT/DELETE/PATCH）」调用 rbac-api
 *       {@code POST /api/rbac/check} 判定用户是否持有该资源；</li>
 *   <li>判定结果计入本地缓存（60 秒短 TTL），减少高频写操作对 rbac-api 的重复调用；</li>
 *   <li>未授权返回 403；rbac-api 不可用时降级放行（仅告警），保证网关可用性；</li>
 *   <li>游客与会员请求不做 RBAC 判定：会员数据归属由各服务自身校验（见订单/购物车等接口）。</li>
 * </ol>
 * 本过滤器在 JWT 认证过滤器（order=-100）之后执行，读取其注入的 X-User-Id / X-Audience 头。
 */
@Slf4j
@Component
public class RbacAuthGlobalFilter implements GlobalFilter, Ordered {

    /** 在 JWT 认证过滤器之后、路由转发之前执行 */
    private static final int ORDER = -90;

    /** rbac-api 授权判定端点（经负载均衡调用） */
    private static final String RBAC_CHECK_URI = "lb://FCM-rbac-microservice/api/rbac/check";

    /** 需要授权判定的写操作方法 */
    private static final Set<HttpMethod> WRITE_METHODS = Set.of(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH);

    /** 会员专属接口前缀（收藏/购物车/地址/通知等「我的数据」）：
     *  资源体系不包含此类接口，后台令牌不做 RBAC 判定，放行到服务层后
     *  由服务端校验身份拒绝（提示「该操作仅限会员」） */
    private static final List<String> MEMBER_ONLY_PREFIXES = List.of(
            "/api/collects", "/api/carts", "/api/member-addresses", "/api/notices");

    /** 判定结果本地缓存 TTL，权限调整后最多 60 秒内生效 */
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    /** 调用 rbac-api 的超时时间，超时走降级放行 */
    private static final Duration REMOTE_TIMEOUT = Duration.ofSeconds(2);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    /** 缓存 key：userId|method|path；value：是否放行 */
    private final Cache<String, Boolean> allowCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(CACHE_TTL)
            .build();

    public RbacAuthGlobalFilter(ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        // 仅后台令牌的写操作需要授权判定（游客/会员请求直接放行）
        if (method == null || !WRITE_METHODS.contains(method)
                || !CommonConstants.AUDIENCE_ADMIN.equals(
                        request.getHeaders().getFirst(CommonConstants.HEADER_AUDIENCE))) {
            return chain.filter(exchange);
        }
        String userId = request.getHeaders().getFirst(CommonConstants.HEADER_USER_ID);
        if (userId == null) {
            // 理论不可达：后台令牌经 JWT 过滤器必然注入用户 ID
            return forbidden(exchange, "缺少用户身份");
        }
        String path = request.getURI().getPath();
        // 会员专属接口不做 RBAC 判定（见 MEMBER_ONLY_PREFIXES 注释）
        if (isMemberOnly(path)) {
            return chain.filter(exchange);
        }
        String cacheKey = userId + "|" + method.name() + "|" + path;
        Boolean cached = allowCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached ? chain.filter(exchange) : forbidden(exchange, "无权限访问该资源");
        }
        return checkRemote(userId, path, method.name())
                .map(allowed -> {
                    allowCache.put(cacheKey, allowed);
                    return allowed;
                })
                .onErrorResume(e -> {
                    // rbac-api 不可用降级放行（与黑名单检查策略一致），仅告警不阻断
                    log.error("RBAC 授权校验异常，降级放行 userId={}, {} {}: {}",
                            userId, method.name(), path, e.getMessage());
                    return Mono.just(true);
                })
                .flatMap(allowed -> allowed
                        ? chain.filter(exchange)
                        : forbidden(exchange, "无权限访问该资源"));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 会员专属接口前缀匹配（精确路径或以「前缀/」开头） */
    private boolean isMemberOnly(String path) {
        return MEMBER_ONLY_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /** 调用 rbac-api 判定（body：{userId, path, method}）；异常由上层降级处理 */
    private Mono<Boolean> checkRemote(String userId, String path, String method) {
        Map<String, String> body = Map.of("userId", userId, "path", path, "method", method);
        return webClient.post()
                .uri(RBAC_CHECK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REMOTE_TIMEOUT)
                .map(this::parseAllowed);
    }

    /** 解析 rbac-api 统一响应：success=true 且 data=true 才放行 */
    private boolean parseAllowed(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("success").asBoolean(false) && root.path("data").asBoolean(false);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RBAC 响应解析失败: " + e.getMessage(), e);
        }
    }

    /** 统一 403 响应：{code:403, success:false, msg, data:null}，与业务响应结构一致 */
    private Mono<Void> forbidden(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(JsonResp.error(ResponseCode.FORBIDDEN, msg));
        } catch (JsonProcessingException e) {
            body = "{\"code\":403,\"success\":false,\"msg\":\"无权限访问该资源\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
