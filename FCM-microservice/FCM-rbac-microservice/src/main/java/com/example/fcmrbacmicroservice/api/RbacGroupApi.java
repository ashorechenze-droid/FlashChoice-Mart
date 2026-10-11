package com.example.fcmrbacmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.RbacGroupEntity;
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
@RequestMapping("api/groups")
public class RbacGroupApi {

    private final RbacService rbacService;

    public RbacGroupApi(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    public JsonResp list() {
        return JsonResp.ok(rbacService.listGroups());
    }

    @GetMapping("/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(rbacService.getGroupById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody RbacGroupEntity group) {
        return JsonResp.ok(rbacService.insertGroup(group));
    }

    @PutMapping
    public JsonResp update(@RequestBody RbacGroupEntity group) {
        return JsonResp.ok(rbacService.updateGroup(group));
    }

    /** 删除（存在用户/角色引用时拒绝） */
    @DeleteMapping("/{id}")
    public JsonResp delete(@PathVariable Long id) {
        return JsonResp.ok(rbacService.deleteGroup(id));
    }
}
