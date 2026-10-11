package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.UserEntity;
import com.example.fcmspi.model.search.UserSearchBean;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 后台用户业务接口（user-api）：CRUD、后台登录
 */
public interface UserService {

    PageInfo<UserEntity> page(UserSearchBean search, PaginateInfo page);

    UserEntity getById(Long id);

    UserEntity getByUsername(String username);

    /** 后台登录：校验用户名/密码/启用/锁定/过期（BCrypt），签发令牌由 api 层完成 */
    UserEntity login(String username, String rawPassword);

    int insert(UserEntity user);

    int update(UserEntity user);

    int updateEnabled(Long id, boolean enabled);

    /** 重置密码（BCrypt 加密） */
    int resetPassword(Long id, String newPassword);

    int deleteByIds(List<Long> ids);
}
