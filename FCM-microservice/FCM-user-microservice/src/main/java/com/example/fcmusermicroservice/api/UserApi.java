package com.example.fcmusermicroservice.api;

import com.example.fcmcommon.dto.TokenPair;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.service.CaptchaService;
import com.example.fcmspi.model.UserEntity;
import com.example.fcmspi.model.search.UserSearchBean;
import com.example.fcmspi.service.UserService;
import com.example.fcmusermicroservice.dto.LoginResult;
import com.example.fcmusermicroservice.service.AuthTokenService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/users")
public class UserApi {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final CaptchaService captchaService;

    public UserApi(UserService userService, AuthTokenService authTokenService,
                   CaptchaService captchaService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
        this.captchaService = captchaService;
    }

    /** 获取图形验证码（公开）：返回 `{captchaId, imageBase64}`，登录时回传校验（2 分钟有效一次性） */
    @GetMapping("/captcha")
    public JsonResp captcha() {
        return JsonResp.ok(captchaService.generate());
    }

    /** 后台用户分页（关键字：用户名） */
    @GetMapping
    public JsonResp page(UserSearchBean search,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        return JsonResp.ok(userService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(userService.getById(id));
    }

    /** 后台登录（公开）：先校验图形验证码，再校验用户名/密码/启用/锁定/过期后签发双 Token（JWT） */
    @PostMapping("/login")
    public JsonResp login(@RequestBody Map<String, String> body) {
        captchaService.validate(body.get("captchaId"), body.get("captcha"));
        UserEntity user = userService.login(body.get("username"), body.get("password"));
        TokenPair pair = authTokenService.issue(user);
        return JsonResp.ok(new LoginResult(user, pair.getAccessToken(), pair.getRefreshToken(),
                pair.getTokenType(), pair.getExpiresIn()));
    }

    @PostMapping
    public JsonResp insert(@RequestBody UserEntity user) {
        return JsonResp.ok(userService.insert(user));
    }

    @PutMapping
    public JsonResp update(@RequestBody UserEntity user) {
        return JsonResp.ok(userService.update(user));
    }

    /** 启用/禁用 */
    @PutMapping("/{id}/enabled")
    public JsonResp updateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return JsonResp.ok(userService.updateEnabled(id, enabled));
    }

    /** 重置密码（BCrypt 加密） */
    @PutMapping("/{id}/reset-password")
    public JsonResp resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        return JsonResp.ok(userService.resetPassword(id, newPassword));
    }

    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids) {
        return JsonResp.ok(userService.deleteByIds(ids));
    }
}
