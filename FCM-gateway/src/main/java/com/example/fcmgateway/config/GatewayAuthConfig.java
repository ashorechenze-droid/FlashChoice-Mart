package com.example.fcmgateway.config;

import com.example.fcmcommon.config.JwtProperties;
import com.example.fcmcommon.util.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关安全配置：
 * WebFlux 环境下 FCM-common 的自动配置不生效（其限定 Servlet），
 * 此处显式注册 JWT 令牌服务与放行配置（密钥须与签发服务保持一致）
 *
 * <p>说明：Spring Boot 4 默认仅注册 Jackson 3（tools.jackson）的 JsonMapper，
 * 而全局过滤器使用 Jackson 2 API 序列化统一 401 响应，故在此显式注册一个
 * Jackson 2 ObjectMapper（类路径由 nacos-client 提供）；@ConditionalOnMissingBean
 * 保证未来若框架自动注册了同类型 Bean 时不冲突。
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class, InternalApiProperties.class})
public class GatewayAuthConfig {

    @Bean
    public JwtTokenService jwtTokenService(JwtProperties jwtProperties) {
        return new JwtTokenService(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
