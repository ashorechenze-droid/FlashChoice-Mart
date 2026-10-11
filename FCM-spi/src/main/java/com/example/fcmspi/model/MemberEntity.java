package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员（member，密码 BCrypt 加密）
 */
@Data
public class MemberEntity extends AuditEntity {

    /** 登录账号（唯一） */
    private String account;
    /** 密码（BCrypt） */
    private String password;
    /** 是否启用 */
    private Boolean enabled;
    /** 姓名 */
    private String name;
    /** 姓名拼音 */
    private String pinyin;
    /** 拼音（无音调） */
    private String pinyinUntoned;
    /** 性别 */
    private String sex;
    /** 生日（yyyy-MM-dd） */
    private String birthday;
    /** 身高 */
    private Integer height;
    /** 体重 */
    private BigDecimal weight;
    /** IQ */
    private Integer iq;
    /** QQ */
    private String qq;
    /** 微信 */
    private String wechat;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 籍贯（地区 ID） */
    private Long nativePlaceId;
    /** 证件号 */
    private String cardId;
    /** 婚姻状况 */
    private String wedlock;
    /** 政治面貌 */
    private String politicalOrientation;
    /** 现住地区 ID */
    private Long addressId;
    /** 详细地址 */
    private String addressDetail;
    /** 民族 */
    private String race;
    /** 宗教 */
    private String religion;
    /** 国籍 */
    private String nationality;
    /** 头像 */
    private String portrait;
    /** 简介 */
    private String description;
    /** 扩展信息（JSON） */
    private String extraInfo;
    /** 最近登录时间 */
    private String lastLoginTime;
    /** 最近登录 IP */
    private String lastLoginIp;
    /** 乐观锁版本 */
    private Integer version;
}
