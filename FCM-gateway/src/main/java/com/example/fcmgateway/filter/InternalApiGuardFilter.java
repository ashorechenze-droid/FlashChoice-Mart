package com.example.fcmgateway.filter;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmgateway.config.InternalApiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 内部接口外部访问拦截过滤器（网关最外层，order=-110）
 *
 * <p>库存扣减/回补、超时关单、RBAC 判定等内部接口仅供服务间 Feign 直连调用，
 * 命中 {@code fcm.gateway.internal-paths} 规则的外部请求直接返回 403，
 * 从网关层收口越权入口（配合各服务业务层校验构成双层防护）。
 */
@Slf4j
@Component
public class InternalApiGuardFilter implements GlobalFilter, Ordered {

    /** 网关最外层执行：先于限流与认证过滤器 */
    private static final int ORDER = -110;

    private final InternalApiProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public InternalApiGuardFilter(InternalApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = String.valueOf(exchange.getRequest().getMethod());
        for (String rule : properties.getInternalPaths()) {
            int space = rule.indexOf(' ');
            if (space <= 0) {
                continue;
            }
            if (rule.substring(0, space).trim().equalsIgnoreCase(method)
                    && pathMatcher.match(rule.substring(space + 1).trim(), path)) {
                log.warn("网关拦截内部接口外部访问 {} {}", method, path);
                return forbidden(exchange);
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 统一 403 响应：{code:403, success:false, msg, data:null}，与业务响应结构一致 */
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    JsonResp.error(ResponseCode.FORBIDDEN, "内部接口禁止外部访问"));
        } catch (JsonProcessingException e) {
            body = "{\"code\":403,\"success\":false,\"msg\":\"内部接口禁止外部访问\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
