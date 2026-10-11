package com.example.fcmspi.service;

import com.example.fcmspi.model.RbacGroupEntity;
import com.example.fcmspi.model.RbacMenuEntity;
import com.example.fcmspi.model.RbacPermEntity;
import com.example.fcmspi.model.RbacResourceEntity;
import com.example.fcmspi.model.RbacRoleEntity;

import java.util.List;

/**
 * RBAC 业务接口（rbac-api）：主数据 CRUD、关系维护（覆盖式）、权限判定、动态菜单
 */
public interface RbacService {

    /* ===== 用户组 ===== */

    List<RbacGroupEntity> listGroups();

    RbacGroupEntity getGroupById(Long id);

    int insertGroup(RbacGroupEntity group);

    int updateGroup(RbacGroupEntity group);

    /** 删除（实现层校验组-用户/组-角色引用） */
    int deleteGroup(Long id);

    /* ===== 角色 ===== */

    List<RbacRoleEntity> listRoles();

    RbacRoleEntity getRoleById(Long id);

    int insertRole(RbacRoleEntity role);

    int updateRole(RbacRoleEntity role);

    /** 删除（实现层校验组-角色/角色-权限引用） */
    int deleteRole(Long id);

    /* ===== 权限 ===== */

    List<RbacPermEntity> listPerms();

    RbacPermEntity getPermById(Long id);

    int insertPerm(RbacPermEntity perm);

    int updatePerm(RbacPermEntity perm);

    /** 删除（实现层校验角色-权限/权限-资源引用） */
    int deletePerm(Long id);

    /* ===== 资源 ===== */

    /** 资源列表（type 为空查全部；type：接口/按钮） */
    List<RbacResourceEntity> listResources(String type);

    RbacResourceEntity getResourceById(Long id);

    int insertResource(RbacResourceEntity resource);

    int updateResource(RbacResourceEntity resource);

    int deleteResource(Long id);

    /* ===== 菜单 ===== */

    /** 全部菜单树（后台管理维护用） */
    List<RbacMenuEntity> menuTree();

    RbacMenuEntity getMenuById(Long id);

    int insertMenu(RbacMenuEntity menu);

    int updateMenu(RbacMenuEntity menu);

    int deleteMenu(Long id);

    /* ===== 关联关系维护（覆盖式：先清后插，事务内执行） ===== */

    /** 为用户分配用户组（t_rbac_user_group） */
    int assignGroupsToUser(Long userId, List<Long> groupIds);

    List<Long> listGroupIdsByUserId(Long userId);

    /** 清空用户的组关联（user-api 删除用户前级联清理，返回清理前关联数） */
    int clearGroupsOfUser(Long userId);

    /** 为用户组分配角色（t_rbac_group_role） */
    int assignRolesToGroup(Long groupId, List<Long> roleIds);

    List<Long> listRoleIdsByGroupId(Long groupId);

    /** 为角色分配权限（t_rbac_role_perm） */
    int assignPermsToRole(Long roleId, List<Long> permIds);

    List<Long> listPermIdsByRoleId(Long roleId);

    /** 为权限分配资源（t_rbac_perm_resource） */
    int assignResourcesToPerm(Long permId, List<Long> resourceIds);

    List<Long> listResourceIdsByPermId(Long permId);

    /* ===== 授权（网关内部调用 / 动态菜单） ===== */

    /**
     * 权限判定：path + method → 资源 → 权限 → 角色 → 用户组 → 用户
     * （网关对后台写接口调用 /api/rbac/check 使用）
     */
    boolean checkPermission(Long userId, String path, String method);

    /** 用户可见菜单树（动态菜单 /api/menus/mine） */
    List<RbacMenuEntity> menusOfUser(Long userId);

    /** 用户可访问资源标识列表（按钮/接口 value，供前端按钮级控制） */
    List<String> listResourceValuesOfUser(Long userId);
}
