package com.example.fcmcommon.service;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.dto.CaptchaResult;
import com.example.fcmcommon.exception.BizException;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

/**
 * 图形验证码服务（登录/注册防刷，参照 eldercare 项目机制）
 *
 * <ul>
 *   <li>生成：SpecCaptcha 绘制图片，文本转小写存 Redis，Key = fcm:auth:captcha:{captchaId}，TTL 2 分钟；</li>
 *   <li>校验：按 captchaId 取存值忽略大小写比对，成功后立即删除（防止同一验证码重复使用）。</li>
 * </ul>
 * 由 FcmCommonAutoConfiguration 在含 Redis 客户端的服务自动注册（member-api / user-api）
 */
@Slf4j
public class CaptchaService {

    /** Redis 存储 Key 前缀（完整 Key：fcm:auth:captcha:{captchaId}） */
    public static final String CAPTCHA_KEY_PREFIX = "fcm:auth:captcha:";
    /** 验证码有效期（分钟），过期需刷新后重试 */
    private static final long CAPTCHA_TTL_MINUTES = 2;

    private final StringRedisTemplate redisTemplate;

    public CaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成验证码（120x40，5 位字符）：返回 captchaId（UUID）与 Base64 图片
     */
    public CaptchaResult generate() {
        SpecCaptcha specCaptcha = new SpecCaptcha(120, 40, 5);
        // 文本统一转小写存储，校验时忽略大小写（与 eldercare 一致）
        String captchaText = specCaptcha.text().toLowerCase();
        String captchaId = UUID.randomUUID().toString();
        redisTemplate.opsForValue()
                .set(CAPTCHA_KEY_PREFIX + captchaId, captchaText, Duration.ofMinutes(CAPTCHA_TTL_MINUTES));
        log.debug("验证码生成成功，ID：{}", captchaId);
        // toBase64 返回 "data:image/png;base64,xxx"，前端可直接作为 img.src
        return new CaptchaResult(captchaId, specCaptcha.toBase64());
    }

    /**
     * 校验验证码：不存在→已过期；不匹配→错误；验证通过后删除（防止重复使用）
     *
     * @param captchaId 获取验证码接口返回的 ID
     * @param captcha   用户输入的验证码文本
     */
    public void validate(String captchaId, String captcha) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new BizException(ResponseCode.CAPTCHA_INVALID, "验证码 ID 不能为空");
        }
        String stored = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + captchaId);
        if (stored == null) {
            throw new BizException(ResponseCode.CAPTCHA_EXPIRED, "图形验证码已过期，请刷新后重试");
        }
        if (captcha == null || !stored.equalsIgnoreCase(captcha)) {
            throw new BizException(ResponseCode.CAPTCHA_INVALID, "图形验证码错误");
        }
        redisTemplate.delete(CAPTCHA_KEY_PREFIX + captchaId);
    }
}
