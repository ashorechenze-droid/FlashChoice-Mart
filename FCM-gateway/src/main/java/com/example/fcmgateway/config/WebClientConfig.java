package com.example.fcmgateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 网关 WebClient 配置
 *
 * <p>Spring Boot 4 将 WebClient 的自动配置拆分至独立模块（spring-boot-webclient），
 * 网关未引入该模块、容器中不存在 WebClient.Builder，故此处显式注册；
 * 并加 {@link LoadBalanced} 使 {@code lb://服务名} 寻址经 Spring Cloud LoadBalancer
 * 解析至具体实例（供 RbacAuthGlobalFilter 调用 rbac-api {@code POST /api/rbac/check} 使用）。
 */
@Configuration
public class WebClientConfig {

    /** 负载均衡 WebClient 构造器：支持 lb://服务名 形式的内部服务调用 */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
