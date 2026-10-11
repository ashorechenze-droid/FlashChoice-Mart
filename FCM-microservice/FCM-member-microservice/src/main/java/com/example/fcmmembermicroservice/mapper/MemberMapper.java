package com.example.fcmmembermicroservice.mapper;

import com.example.fcmspi.model.MemberEntity;
import com.example.fcmspi.model.search.MemberSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberMapper {

    List<MemberEntity> findAll(MemberSearchBean search);

    /** 按 ID 查询（不含 password） */
    MemberEntity findById(Long id);

    /** 按 ID 查询（含 password，修改密码校验用） */
    MemberEntity findByIdWithPassword(Long id);

    /** 按账号查询（含 password，登录校验用） */
    MemberEntity findByAccountWithPassword(String account);

    /** 更新最近登录时间与登录 IP */
    int updateLastLogin(MemberEntity member);

    int save(MemberEntity member);

    /** 乐观锁更新：where id + version，version 自增 */
    int update(MemberEntity member);

    int updatePassword(Long id, String password);

    int updateEnabled(Long id, boolean enabled);
}
