package com.example.fcmrbacmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.RbacResourceEntity;
import com.example.fcmspi.service.RbacService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/resources")
public class RbacResourceApi {

    private final RbacService rbacService;

    public RbacResourceApi(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /** 资源列表（type：接口/按钮，不传查全部） */
    @GetMapping
    public JsonResp list(@RequestParam(required = false) String type) {
        return JsonResp.ok(rbacService.listResources(type));
    }

    @GetMapping("/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(rbacService.getResourceById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody RbacResourceEntity resource) {
        return JsonResp.ok(rbacService.insertResource(resource));
    }

    @PutMapping
    public JsonResp update(@RequestBody RbacResourceEntity resource) {
        return JsonResp.ok(rbacService.updateResource(resource));
    }

    /** 删除（存在权限引用/菜单引用时拒绝） */
    @DeleteMapping("/{id}")
    public JsonResp delete(@PathVariable Long id) {
        return JsonResp.ok(rbacService.deleteResource(id));
    }
}
