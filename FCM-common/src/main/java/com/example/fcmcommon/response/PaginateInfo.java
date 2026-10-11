package com.example.fcmcommon.response;

import lombok.Data;

/**
 * 分页请求参数。
 * 约定：pageNo 默认 1；pageSize 默认 0（配合 PageHelper page-size-zero: on 表示查询全部）
 */
@Data
public class PaginateInfo {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 0;

    private Integer pageNo = DEFAULT_PAGE_NO;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private PaginateInfo() {
    }

    public static PaginateInfo from(int pageNo, int pageSize) {
        PaginateInfo info = new PaginateInfo();
        info.pageNo = pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
        info.pageSize = pageSize < 0 ? DEFAULT_PAGE_SIZE : pageSize;
        return info;
    }
}
