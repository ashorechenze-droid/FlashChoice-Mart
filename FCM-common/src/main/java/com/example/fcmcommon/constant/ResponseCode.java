package com.example.fcmcommon.constant;

/**
 * 统一错误码（详见详细设计文档附录 A 错误码定义）
 */
public final class ResponseCode {

    private ResponseCode() {
    }

    /** 成功 */
    public static final int SUCCESS = 200;
    /** 参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未登录或令牌失效 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 请求过于频繁/限流 */
    public static final int TOO_MANY_REQUESTS = 429;
    /** 系统内部错误 */
    public static final int INTERNAL_ERROR = 500;
    /** 库存不足 */
    public static final int STOCK_NOT_ENOUGH = 7001;
    /** 未到秒杀时间或已结束 */
    public static final int SECKILL_NOT_OPEN = 7002;
    /** 已参与过该秒杀/超出限购 */
    public static final int SECKILL_ALREADY_JOINED = 7003;
    /** 秒杀已售罄 */
    public static final int SECKILL_SOLD_OUT = 7004;
    /** 订单状态不允许该操作 */
    public static final int ORDER_STATUS_INVALID = 7005;
    /** 图形验证码错误 */
    public static final int CAPTCHA_INVALID = 7006;
    /** 图形验证码已过期 */
    public static final int CAPTCHA_EXPIRED = 7007;
}
