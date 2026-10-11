package com.example.fcmusermicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * RBAC 服务 Feign 客户端（user-api 删除用户前清理组-用户关联）
 * 对应 FCM-rbac-microservice 的 DELETE /api/rbac/users/{userId}/groups
 */
@FeignClient(name = "FCM-rbac-microservice", path = "api/rbac")
public interface RbacFeignClient {

    /** 清空用户的组关联（级联清理 t_rbac_user_group），data 为清理前的关联数 */
    @DeleteMapping("/users/{userId}/groups")
    TypedJsonResp<Integer> clearGroupsOfUser(@PathVariable("userId") Long userId);
}
