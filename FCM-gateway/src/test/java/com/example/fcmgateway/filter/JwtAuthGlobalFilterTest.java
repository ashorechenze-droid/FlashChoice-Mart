package com.example.fcmgateway.filter;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.TokenConstants;
import com.example.fcmcommon.dto.TokenPair;
import com.example.fcmcommon.util.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关 JWT 过滤器集成测试（真实连接本地 Redis）
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>有效令牌 + 命中用户级黑名单（登出/改密/禁用后）→ 401，不转发下游；</li>
 *   <li>有效令牌 + 无黑名单 → 转发下游并注入身份请求头；</li>
 *   <li>Refresh Token 访问业务接口 → 401，不转发下游（仅 Access Token 可访问）。</li>
 * </ol>
 * 需本地 Redis 正常运行（application.yaml 中的连接与密码配置须正确）。
 */
@SpringBootTest
class JwtAuthGlobalFilterTest {

    private static final long TEST_USER_ID = 99999L;

    @Autowired
    private JwtAuthGlobalFilter filter;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Test
    void blacklistedTokenShouldBeRejectedWith401() {
        TokenPair pair = jwtTokenService.generateTokenPair(TEST_USER_ID, "test-user", CommonConstants.AUDIENCE_MEMBER);
        String token = pair.getAccessToken();

        String blacklistKey = TokenConstants.REDIS_BLACKLIST_USER_KEY + TEST_USER_ID;
        redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofSeconds(60)).block();
        try {
            MockServerWebExchange exchange = exchangeWithToken(token);
            AtomicBoolean chained = new AtomicBoolean(false);
            GatewayFilterChain chain = ex -> {
                chained.set(true);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertThat(chained.get()).as("黑名单命中时不应转发下游").isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } finally {
            redisTemplate.delete(blacklistKey).block();
        }
    }

    @Test
    void refreshTokenShouldBeRejectedWith401() {
        TokenPair pair = jwtTokenService.generateTokenPair(TEST_USER_ID, "test-user", CommonConstants.AUDIENCE_MEMBER);

        MockServerWebExchange exchange = exchangeWithToken(pair.getRefreshToken());
        AtomicBoolean chained = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chained.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chained.get()).as("Refresh Token 不应转发下游").isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenShouldPassAndInjectIdentityHeaders() {
        TokenPair pair = jwtTokenService.generateTokenPair(TEST_USER_ID, "test-user", CommonConstants.AUDIENCE_MEMBER);
        String token = pair.getAccessToken();

        MockServerWebExchange exchange = exchangeWithToken(token);

        AtomicBoolean chained = new AtomicBoolean(false);
        final ServerWebExchange[] forwarded = new ServerWebExchange[1];
        GatewayFilterChain chain = ex -> {
            chained.set(true);
            forwarded[0] = ex;
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chained.get()).as("有效令牌应转发下游").isTrue();
        assertThat(forwarded[0]).isNotNull();
        assertThat(forwarded[0].getRequest().getHeaders().getFirst(CommonConstants.HEADER_USER_ID))
                .isEqualTo(String.valueOf(TEST_USER_ID));
        assertThat(forwarded[0].getRequest().getHeaders().getFirst(CommonConstants.HEADER_AUDIENCE))
                .isEqualTo(CommonConstants.AUDIENCE_MEMBER);
    }

    private MockServerWebExchange exchangeWithToken(String token) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/carts")
                .header(TokenConstants.HEADER_AUTHORIZATION, TokenConstants.BEARER_PREFIX + token)
                .build();
        return MockServerWebExchange.from(request);
    }
}
