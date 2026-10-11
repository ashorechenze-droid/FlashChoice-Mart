package com.example.fcmspi.model.search;

import com.example.fcmspi.model.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台用户查询条件：支持用户名关键字与 enabled 等继承字段过滤
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSearchBean extends UserEntity {

    /** 关键字：匹配 username */
    private String keyword;
}
