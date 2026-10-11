package com.example.fcmgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证放行配置（fcm.auth.*）
 *
 * <p>公开接口白名单依据《详细设计说明书》4.1 通用约定：
 * 登录/注册、刷新令牌（Access Token 过期后仍需换新，必须公开）、
 * 商品与分类查询（GET）、地区查询、秒杀活动查询、文件读取。
 * 注意：登录/刷新/注册之外的写接口一律需要携带有效 Access Token（Authorization: Bearer）。
 */
@Data
@ConfigurationProperties(prefix = "fcm.auth")
public class AuthProperties {

    /**
     * 无需登录的公开路径（精确路径或 ant 通配，任意 HTTP 方法）。
     * 示例：/api/users/login、/api/members/register、/api/members/refresh
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 游客可 GET 的公开查询前缀（完整前缀，如 /api/goods）。
     * 匹配规则：path 等于前缀或以"前缀/"开头，且请求方法为 GET。
     * 这些域的管理写接口（POST/PUT/DELETE）仍需登录。
     */
    private List<String> publicGetPrefixes = new ArrayList<>();
}
