package com.example.fcmgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关内部接口保护配置（fcm.gateway.*）
 *
 * <p>内部接口仅供服务间调用（Feign 直连不经网关），规则格式为 "METHOD 路径模式"（ant），
 * 命中即拒绝外部访问（403）。
 */
@Data
@ConfigurationProperties(prefix = "fcm.gateway")
public class InternalApiProperties {

    /** 内部接口规则，如 "PUT /api/goods/stock/deduct"、"PUT /api/orders/close-timeout/**" */
    private List<String> internalPaths = new ArrayList<>();
}
