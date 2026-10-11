package com.example.fcmrbacmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.RbacMenuEntity;
import com.example.fcmspi.service.RbacService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/menus")
public class RbacMenuApi {

    private final RbacService rbacService;

    public RbacMenuApi(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /** 后台维护：全部菜单树 */
    @GetMapping("/tree")
    public JsonResp tree() {
        return JsonResp.ok(rbacService.menuTree());
    }

    /** 当前登录用户动态菜单（按授权资源过滤，X-User-Id 由网关注入） */
    @GetMapping("/mine")
    public JsonResp mine(@RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerUserId,
                         @RequestParam(required = false) Long userId) {
        Long uid = headerUserId != null ? Long.valueOf(headerUserId) : userId;
        return JsonResp.ok(uid == null ? null : rbacService.menusOfUser(uid));
    }

    @GetMapping("/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(rbacService.getMenuById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody RbacMenuEntity menu) {
        return JsonResp.ok(rbacService.insertMenu(menu));
    }

    @PutMapping
    public JsonResp update(@RequestBody RbacMenuEntity menu) {
        return JsonResp.ok(rbacService.updateMenu(menu));
    }

    /** 删除（存在子菜单时拒绝） */
    @DeleteMapping("/{id}")
    public JsonResp delete(@PathVariable Long id) {
        return JsonResp.ok(rbacService.deleteMenu(id));
    }
}
