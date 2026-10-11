package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.MemberEntity;
import com.example.fcmspi.model.search.MemberSearchBean;
import com.github.pagehelper.PageInfo;

/**
 * 会员业务接口（member-api）：注册/登录、资料维护、启用禁用、重置密码
 */
public interface MemberService {

    PageInfo<MemberEntity> page(MemberSearchBean search, PaginateInfo page);

    MemberEntity getById(Long id);

    MemberEntity getByAccount(String account);

    /** 登录校验（BCrypt），失败抛 BizException；签发令牌由 api 层完成 */
    MemberEntity login(String account, String rawPassword);

    /** 注册（账号唯一校验 + BCrypt 加密，实现层填充） */
    int register(MemberEntity member);

    /** 资料更新（乐观锁 version 校验） */
    int update(MemberEntity member);

    /** 修改密码（校验原密码） */
    int updatePassword(Long id, String oldPassword, String newPassword);

    int updateEnabled(Long id, boolean enabled);

    /** 重置密码（后台） */
    int resetPassword(Long id, String newPassword);
}
