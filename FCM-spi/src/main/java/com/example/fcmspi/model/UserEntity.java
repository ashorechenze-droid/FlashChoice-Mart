package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 后台用户（user，密码 BCrypt 加密）
 */
@Data
public class UserEntity extends AuditEntity {

    /** 用户名（唯一） */
    private String username;
    /** 密码（BCrypt） */
    private String password;
    /** 头像 */
    private String avatarUrl;
    /** 是否启用 */
    private Boolean enabled;
    /** 账号状态 */
    private Integer status;
    /** 是否锁定 */
    private Boolean locked;
    /** 账号过期时间 */
    private String userExpireTime;
    /** 凭证过期时间 */
    private String credentialExpireTime;
    /** 登录次数 */
    private Integer loginTimes;
    /** 最近登录时间 */
    private String lastLoginTime;
    /** 最近登录 IP */
    private String lastLoginIp;
    /** 操作模式 */
    private Integer opMode;
    /** 说明 */
    private String description;
}
