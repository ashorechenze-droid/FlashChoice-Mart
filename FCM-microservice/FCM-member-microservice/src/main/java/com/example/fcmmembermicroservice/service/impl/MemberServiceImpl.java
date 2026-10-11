package com.example.fcmmembermicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmmembermicroservice.mapper.MemberMapper;
import com.example.fcmmembermicroservice.service.TokenBlacklistService;
import com.example.fcmspi.model.MemberEntity;
import com.example.fcmspi.model.search.MemberSearchBean;
import com.example.fcmspi.service.MemberService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MemberServiceImpl implements MemberService {

    /** 本服务令牌类型（会员，吊销黑名单用） */
    private static final String AUDIENCE = CommonConstants.AUDIENCE_MEMBER;

    private final MemberMapper memberMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberServiceImpl(MemberMapper memberMapper, TokenBlacklistService tokenBlacklistService) {
        this.memberMapper = memberMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
    }

    @Override
    public PageInfo<MemberEntity> page(MemberSearchBean search, PaginateInfo page) {
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        return new PageInfo<>(memberMapper.findAll(search));
    }

    @Override
    public MemberEntity getById(Long id) {
        return memberMapper.findById(id);
    }

    @Override
    public MemberEntity getByAccount(String account) {
        return memberMapper.findByAccountWithPassword(account);
    }

    @Override
    public MemberEntity login(String account, String rawPassword) {
        MemberEntity member = memberMapper.findByAccountWithPassword(account);
        if (member == null || !passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "账号或密码错误");
        }
        if (!Boolean.TRUE.equals(member.getEnabled())) {
            throw new BizException(ResponseCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        // 更新最近登录信息（IP 由登录上下文补充）
        MemberEntity upd = new MemberEntity();
        upd.setId(member.getId());
        upd.setLastLoginTime(now());
        memberMapper.updateLastLogin(upd);
        // 登录校验查询仅取必要字段，返回前补全完整资料（findById 不含密码）
        return memberMapper.findById(member.getId());
    }

    @Override
    public int register(MemberEntity member) {
        if (memberMapper.findByAccountWithPassword(member.getAccount()) != null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "账号已存在");
        }
        String now = now();
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        if (member.getEnabled() == null) {
            member.setEnabled(true);
        }
        if (member.getVersion() == null) {
            member.setVersion(0);
        }
        member.setCreatedTime(now);
        member.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        return memberMapper.save(member);
    }

    @Override
    public int update(MemberEntity member) {
        if (member.getId() == null || member.getVersion() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少 id 或 version（乐观锁）");
        }
        member.setUpdatedTime(now());
        // 密码不允许走通用更新
        member.setPassword(null);
        int rows = memberMapper.update(member);
        if (rows == 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "资料已被他人修改，请刷新后重试");
        }
        return rows;
    }

    @Override
    public int updatePassword(Long id, String oldPassword, String newPassword) {
        MemberEntity member = memberMapper.findByIdWithPassword(id);
        if (member == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "会员不存在");
        }
        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "原密码错误");
        }
        int rows = memberMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        // 改密后强制重新登录：吊销该会员全部令牌
        if (rows > 0) {
            tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id);
        }
        return rows;
    }

    @Override
    public int updateEnabled(Long id, boolean enabled) {
        int rows = memberMapper.updateEnabled(id, enabled);
        // 禁用即强制下线：吊销该会员全部令牌，防止已签发令牌继续访问
        if (rows > 0 && !enabled) {
            tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id);
        }
        return rows;
    }

    @Override
    public int resetPassword(Long id, String newPassword) {
        int rows = memberMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        // 重置密码后强制重新登录：吊销该会员全部令牌
        if (rows > 0) {
            tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id);
        }
        return rows;
    }
}
