package com.example.fcmcommon.exception;

import com.example.fcmcommon.constant.ResponseCode;
import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由全局异常处理器统一包装为 JsonResp
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ResponseCode.INTERNAL_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
