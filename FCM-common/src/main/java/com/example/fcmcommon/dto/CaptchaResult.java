package com.example.fcmcommon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码结果（登录/注册接口返回给前端）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaResult {

    /** 验证码 ID（UUID），登录/注册时随用户输入的验证码一并回传 */
    private String captchaId;
    /** 验证码图片（Base64 Data URL，前端可直接放入 img 的 src 显示） */
    private String imageBase64;
}
