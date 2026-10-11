package com.example.fcmcommon.config;

import com.example.fcmcommon.exception.GlobalExceptionHandler;
import com.example.fcmcommon.service.CaptchaService;
import com.example.fcmcommon.util.JwtTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * FCM-common 自动配置：
 * 仅在 Servlet Web（MVC）环境下注册全局异常处理器与 JWT 令牌服务，WebFlux 网关不生效
 * （网关为响应式环境，由网关模块自行注册 JwtTokenService）
 *
 * <p>JwtTokenService 仅当配置了 fcm.jwt.secret 时才注册：
 * 签发方（user-api / member-api）需配置该密钥；无 JWT 业务的微服务（如 brand）无需配置即可正常启动
 *
 * <p>注意：引用 classpath 中可能缺失类型的 @Bean 方法必须放在独立的嵌套配置类中，
 * 否则 Spring 反射扫描外层配置类的方法签名时会因缺类（如 StringRedisTemplate）而启动失败
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(JwtProperties.class)
@Import(GlobalExceptionHandler.class)
public class FcmCommonAutoConfiguration {

    /**
     * JWT 令牌服务（签发方：user-api / member-api；校验方网关自行注册同名 Bean）
     * 未配置 fcm.jwt.secret 的服务不注册此 Bean，避免无 JWT 业务的服务被强制要求密钥
     */
    @Bean
    @ConditionalOnProperty(prefix = "fcm.jwt", name = "secret")
    public JwtTokenService jwtTokenService(JwtProperties jwtProperties) {
        return new JwtTokenService(jwtProperties);
    }

    /**
     * 图形验证码服务（登录/注册防刷，基于 Redis 存储）：
     * 仅在 classpath 含 Redis 客户端的服务注册（member-api / user-api），
     * 无 Redis 依赖的微服务（brand/category 等）不受影响
     *
     * <p>条件注解放在嵌套类级别而非 @Bean 方法上：条件不满足时嵌套类不会被加载，
     * 从而避免反射扫描方法签名时因 StringRedisTemplate 缺类导致 NoClassDefFoundError
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    static class CaptchaConfiguration {

        @Bean
        public CaptchaService captchaService(StringRedisTemplate stringRedisTemplate) {
            return new CaptchaService(stringRedisTemplate);
        }
    }
}
