package com.example.fcmcommon.constant;

/**
 * 通用常量：时间格式、网关注入的请求头、令牌类型等
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 日期时间格式（审计字段统一） */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /** 日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 网关解析 JWT 后注入的用户 ID 请求头 */
    public static final String HEADER_USER_ID = "X-User-Id";
    /** 网关解析 JWT 后注入的用户名请求头 */
    public static final String HEADER_USER_NAME = "X-User-Name";
    /** 网关解析 JWT 后注入的令牌类型请求头 */
    public static final String HEADER_AUDIENCE = "X-Audience";

    /** 令牌类型：后台用户 */
    public static final String AUDIENCE_ADMIN = "admin";
    /** 令牌类型：会员 */
    public static final String AUDIENCE_MEMBER = "member";
}
