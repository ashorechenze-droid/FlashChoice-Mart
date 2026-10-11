package com.example.fcmcommon.exception;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.response.JsonResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一包装为 JsonResp，避免堆栈外泄（详见详细设计文档 2.6.4）
 * 由 FcmCommonAutoConfiguration 在 Servlet Web 环境下自动装配
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public JsonResp handleBizException(BizException e) {
        log.warn("业务异常 code={}, msg={}", e.getCode(), e.getMessage());
        return JsonResp.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public JsonResp handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() == null
                ? "参数错误"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return JsonResp.error(ResponseCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public JsonResp handleNotReadableException(HttpMessageNotReadableException e) {
        return JsonResp.error(ResponseCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public JsonResp handleException(Exception e) {
        log.error("系统异常", e);
        return JsonResp.error(ResponseCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }
}
