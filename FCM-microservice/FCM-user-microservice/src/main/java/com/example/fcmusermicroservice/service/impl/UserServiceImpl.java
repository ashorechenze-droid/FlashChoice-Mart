package com.example.fcmusermicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.UserEntity;
import com.example.fcmspi.model.search.UserSearchBean;
import com.example.fcmspi.service.UserService;
import com.example.fcmusermicroservice.feign.RbacFeignClient;
import com.example.fcmusermicroservice.mapper.UserMapper;
import com.example.fcmusermicroservice.service.TokenBlacklistService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 本服务令牌类型（后台用户，吊销黑名单用） */
    private static final String AUDIENCE = CommonConstants.AUDIENCE_ADMIN;

    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final RbacFeignClient rbacFeignClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, TokenBlacklistService tokenBlacklistService,
                           RbacFeignClient rbacFeignClient) {
        this.userMapper = userMapper;
        this.tokenBlacklistService = tokenBlacklistService;
        this.rbacFeignClient = rbacFeignClient;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
    }

    @Override
    public PageInfo<UserEntity> page(UserSearchBean search, PaginateInfo page) {
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        return new PageInfo<>(userMapper.findAll(search));
    }

    @Override
    public UserEntity getById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public UserEntity getByUsername(String username) {
        return userMapper.findByUsernameWithPassword(username);
    }

    @Override
    public UserEntity login(String username, String rawPassword) {
        UserEntity user = userMapper.findByUsernameWithPassword(username);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException(ResponseCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new BizException(ResponseCode.FORBIDDEN, "账号已被锁定，请联系管理员");
        }
        if (user.getUserExpireTime() != null && user.getUserExpireTime().compareTo(now()) < 0) {
            throw new BizException(ResponseCode.FORBIDDEN, "账号已过期");
        }
        // 更新登录次数与最近登录时间（IP 由登录上下文补充）
        UserEntity upd = new UserEntity();
        upd.setId(user.getId());
        upd.setLastLoginTime(now());
        userMapper.updateLoginInfo(upd);
        // 登录校验查询仅取必要字段，返回前补全完整资料（findById 不含密码）
        return userMapper.findById(user.getId());
    }

    @Override
    public int insert(UserEntity user) {
        if (userMapper.findByUsernameWithPassword(user.getUsername()) != null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "用户名已存在");
        }
        String now = now();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        if (user.getLocked() == null) {
            user.setLocked(false);
        }
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        if (user.getLoginTimes() == null) {
            user.setLoginTimes(0);
        }
        if (user.getOpMode() == null) {
            user.setOpMode(0);
        }
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        return userMapper.save(user);
    }

    @Override
    public int update(UserEntity user) {
        user.setUpdatedTime(now());
        // 用户名与密码不允许走通用更新
        user.setUsername(null);
        user.setPassword(null);
        return userMapper.update(user);
    }

    @Override
    public int updateEnabled(Long id, boolean enabled) {
        int rows = userMapper.updateEnabled(id, enabled);
        // 禁用即强制下线：吊销该用户全部令牌，防止已签发令牌继续访问
        if (rows > 0 && !enabled) {
            tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id);
        }
        return rows;
    }

    @Override
    public int resetPassword(Long id, String newPassword) {
        int rows = userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        // 重置密码后强制重新登录：吊销该用户全部令牌
        if (rows > 0) {
            tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id);
        }
        return rows;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 先经 rbac-api 级联清理组-用户关联（t_rbac_user_group），远程失败则阻断删除
        for (Long id : ids) {
            clearUserGroups(id);
        }
        int rows = userMapper.deleteByIds(ids);
        // 删除后吊销其令牌，防止残留令牌访问
        if (rows > 0) {
            ids.forEach(id -> tokenBlacklistService.revokeAllUserTokens(AUDIENCE, id));
        }
        return rows;
    }

    /** 清空用户组关联；rbac 服务不可用时抛业务异常，避免残留脏关联 */
    private void clearUserGroups(Long userId) {
        try {
            TypedJsonResp<Integer> resp = rbacFeignClient.clearGroupsOfUser(userId);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BizException(ResponseCode.BAD_REQUEST, "RBAC 服务不可用，暂时无法删除用户");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("RBAC 服务清理用户组关联失败 userId={}: {}", userId, e.getMessage());
            throw new BizException(ResponseCode.BAD_REQUEST, "RBAC 服务不可用，暂时无法删除用户");
        }
    }
}
