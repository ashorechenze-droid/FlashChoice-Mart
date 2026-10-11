package com.example.fcmcommon.response;

import com.example.fcmcommon.constant.ResponseCode;
import lombok.Data;

/**
 * 统一响应结构（data 为 Object），用于 REST 接口返回
 * { "code":200, "success":true, "msg":"success", "data":{...} }
 */
@Data
public class JsonResp {

    /** 业务码：200 成功，非 200 失败（见错误码表） */
    private Integer code;
    /** 成功/失败标志 */
    private Boolean success;
    /** 提示消息 */
    private String msg;
    /** 业务数据（分页时为 PageInfo） */
    private Object data;

    private JsonResp() {
    }

    public static JsonResp ok() {
        return ok(null);
    }

    public static JsonResp ok(Object data) {
        return ok("success", data);
    }

    public static JsonResp ok(String msg, Object data) {
        JsonResp resp = new JsonResp();
        resp.code = ResponseCode.SUCCESS;
        resp.success = true;
        resp.msg = msg;
        resp.data = data;
        return resp;
    }

    public static JsonResp error(String msg) {
        return error(ResponseCode.INTERNAL_ERROR, msg);
    }

    public static JsonResp error(int code, String msg) {
        JsonResp resp = new JsonResp();
        resp.code = code;
        resp.success = false;
        resp.msg = msg;
        return resp;
    }
}
