package com.example.fcmrbacmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.RbacRoleEntity;
import com.example.fcmspi.service.RbacService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/roles")
public class RbacRoleApi {

    private final RbacService rbacService;

    public RbacRoleApi(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    public JsonResp list() {
        return JsonResp.ok(rbacService.listRoles());
    }

    @GetMapping("/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(rbacService.getRoleById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody RbacRoleEntity role) {
        return JsonResp.ok(rbacService.insertRole(role));
    }

    @PutMapping
    public JsonResp update(@RequestBody RbacRoleEntity role) {
        return JsonResp.ok(rbacService.updateRole(role));
    }

    /** 删除（存在组引用/权限分配时拒绝） */
    @DeleteMapping("/{id}")
    public JsonResp delete(@PathVariable Long id) {
        return JsonResp.ok(rbacService.deleteRole(id));
    }
}
