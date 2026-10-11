package com.example.fcmusermicroservice.mapper;

import com.example.fcmspi.model.UserEntity;
import com.example.fcmspi.model.search.UserSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    List<UserEntity> findAll(UserSearchBean search);

    /** 按 ID 查询（不含 password） */
    UserEntity findById(Long id);

    /** 按 ID 查询（含 password，重置密码场景校验用） */
    UserEntity findByIdWithPassword(Long id);

    /** 按用户名查询（含 password 与状态字段，登录校验用） */
    UserEntity findByUsernameWithPassword(String username);

    /** 更新登录次数/最近登录时间与 IP */
    int updateLoginInfo(UserEntity user);

    int save(UserEntity user);

    int update(UserEntity user);

    int updatePassword(Long id, String password);

    int updateEnabled(Long id, boolean enabled);

    int deleteByIds(List<Long> ids);
}
