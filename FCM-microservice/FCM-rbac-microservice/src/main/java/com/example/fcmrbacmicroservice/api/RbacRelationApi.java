package com.example.fcmrbacmicroservice.api;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.response.JsonResp;
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

import java.util.List;
import java.util.Map;

/**
 * RBAC 关系维护与授权判定：
 * 分配接口为覆盖式（PUT 整组替换）；/api/rbac/check 供网关对后台写接口鉴权调用
 */
@RestController
@RequestMapping("api/rbac")
public class RbacRelationApi {

    private final RbacService rbacService;

    public RbacRelationApi(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /* ===== 用户-组 ===== */

    @PutMapping("/users/{userId}/groups")
    public JsonResp assignGroupsToUser(@PathVariable Long userId, @RequestBody List<Long> groupIds) {
        return JsonResp.ok(rbacService.assignGroupsToUser(userId, groupIds));
    }

    @GetMapping("/users/{userId}/groups")
    public JsonResp listGroupIdsByUserId(@PathVariable Long userId) {
        return JsonResp.ok(rbacService.listGroupIdsByUserId(userId));
    }

    /** 清空用户的组关联（user-api 删除用户前级联清理） */
    @DeleteMapping("/users/{userId}/groups")
    public JsonResp clearGroupsOfUser(@PathVariable Long userId) {
        return JsonResp.ok(rbacService.clearGroupsOfUser(userId));
    }

    /* ===== 组-角色 ===== */

    @PutMapping("/groups/{groupId}/roles")
    public JsonResp assignRolesToGroup(@PathVariable Long groupId, @RequestBody List<Long> roleIds) {
        return JsonResp.ok(rbacService.assignRolesToGroup(groupId, roleIds));
    }

    @GetMapping("/groups/{groupId}/roles")
    public JsonResp listRoleIdsByGroupId(@PathVariable Long groupId) {
        return JsonResp.ok(rbacService.listRoleIdsByGroupId(groupId));
    }

    /* ===== 角色-权限 ===== */

    @PutMapping("/roles/{roleId}/perms")
    public JsonResp assignPermsToRole(@PathVariable Long roleId, @RequestBody List<Long> permIds) {
        return JsonResp.ok(rbacService.assignPermsToRole(roleId, permIds));
    }

    @GetMapping("/roles/{roleId}/perms")
    public JsonResp listPermIdsByRoleId(@PathVariable Long roleId) {
        return JsonResp.ok(rbacService.listPermIdsByRoleId(roleId));
    }

    /* ===== 权限-资源 ===== */

    @PutMapping("/perms/{permId}/resources")
    public JsonResp assignResourcesToPerm(@PathVariable Long permId, @RequestBody List<Long> resourceIds) {
        return JsonResp.ok(rbacService.assignResourcesToPerm(permId, resourceIds));
    }

    @GetMapping("/perms/{permId}/resources")
    public JsonResp listResourceIdsByPermId(@PathVariable Long permId) {
        return JsonResp.ok(rbacService.listResourceIdsByPermId(permId));
    }

    /* ===== 当前用户资源标识（前端按钮权限控制） ===== */

    /** 当前登录用户的资源标识列表（按钮标识如 good:add 与接口路径），X-User-Id 由网关注入 */
    @GetMapping("/mine/resources")
    public JsonResp listMyResourceValues(@RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) String headerUserId,
                                         @RequestParam(required = false) Long userId) {
        Long uid = headerUserId != null ? Long.valueOf(headerUserId) : userId;
        return JsonResp.ok(uid == null ? List.of() : rbacService.listResourceValuesOfUser(uid));
    }

    /* ===== 权限判定（网关内部调用） ===== */

    /** body：{userId, path, method}，method 维度预留 */
    @PostMapping("/check")
    public JsonResp checkPermission(@RequestBody Map<String, String> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(body.get("userId"));
        return JsonResp.ok(rbacService.checkPermission(userId, body.get("path"), body.get("method")));
    }
}
