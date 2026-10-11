package com.example.fcmcommon.response;

import com.example.fcmcommon.constant.ResponseCode;
import lombok.Data;

/**
 * 泛型统一响应结构，data 类型为 T，用于服务间 Feign 调用
 */
@Data
public class TypedJsonResp<T> {

    private Integer code;
    private Boolean success;
    private String msg;
    private T data;

    private TypedJsonResp() {
    }

    public static <T> TypedJsonResp<T> ok(T data) {
        TypedJsonResp<T> resp = new TypedJsonResp<>();
        resp.code = ResponseCode.SUCCESS;
        resp.success = true;
        resp.msg = "success";
        resp.data = data;
        return resp;
    }

    public static <T> TypedJsonResp<T> error(int code, String msg) {
        TypedJsonResp<T> resp = new TypedJsonResp<>();
        resp.code = code;
        resp.success = false;
        resp.msg = msg;
        return resp;
    }
}
