package com.example.fcmmembermicroservice.dto;

import com.example.fcmspi.model.MemberEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员注册请求体：在会员实体基础上追加图形验证码字段，
 * body 结构与接口文档一致（account/password/... 扁平传递 + captchaId/captcha）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberRegisterParam extends MemberEntity {

    /** 验证码 ID（获取验证码接口返回） */
    private String captchaId;
    /** 用户输入的验证码 */
    private String captcha;
}
