package com.example.fcmrbacmicroservice.mapper;

import com.example.fcmspi.model.RbacGroupEntity;
import com.example.fcmspi.model.RbacMenuEntity;
import com.example.fcmspi.model.RbacPermEntity;
import com.example.fcmspi.model.RbacResourceEntity;
import com.example.fcmspi.model.RbacRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RBAC Mapper：主数据（组/角色/权限/资源/菜单）CRUD + 关联表维护 + 授权查询
 */
@Mapper
public interface RbacMapper {

    /* ===== 用户组 t_rbac_group ===== */

    List<RbacGroupEntity> findAllGroups();

    RbacGroupEntity findGroupById(Long id);

    int saveGroup(RbacGroupEntity group);

    int updateGroup(RbacGroupEntity group);

    int deleteGroupById(Long id);

    /** 用户-组 引用计数（删除组前校验） */
    int countUserGroupByGroupId(Long groupId);

    /** 组-角色 引用计数（删除组前校验） */
    int countGroupRoleByGroupId(Long groupId);

    /* ===== 角色 t_rbac_role ===== */

    List<RbacRoleEntity> findAllRoles();

    RbacRoleEntity findRoleById(Long id);

    int saveRole(RbacRoleEntity role);

    int updateRole(RbacRoleEntity role);

    int deleteRoleById(Long id);

    /** 组-角色 引用计数（删除角色前校验） */
    int countGroupRoleByRoleId(Long roleId);

    /** 角色-权限 引用计数（删除角色前校验） */
    int countRolePermByRoleId(Long roleId);

    /* ===== 权限 t_rbac_perm ===== */

    List<RbacPermEntity> findAllPerms();

    RbacPermEntity findPermById(Long id);

    int savePerm(RbacPermEntity perm);

    int updatePerm(RbacPermEntity perm);

    int deletePermById(Long id);

    /** 角色-权限 引用计数（删除权限前校验） */
    int countRolePermByPermId(Long permId);

    /** 权限-资源 引用计数（删除权限前校验） */
    int countPermResourceByPermId(Long permId);

    /* ===== 资源 t_rbac_resource ===== */

    /** type 为空查全部（type：接口/按钮） */
    List<RbacResourceEntity> findAllResources(String type);

    RbacResourceEntity findResourceById(Long id);

    int saveResource(RbacResourceEntity resource);

    int updateResource(RbacResourceEntity resource);

    int deleteResourceById(Long id);

    /** 权限-资源 引用计数（删除资源前校验） */
    int countPermResourceByResourceId(Long resourceId);

    /** 菜单-资源 引用计数（删除资源前校验） */
    int countMenuByResourceId(Long resourceId);

    /* ===== 菜单 t_rbac_menu ===== */

    /** 全部菜单（树组装在 Service 层，parent_id=0 为根，按 sort 排序） */
    List<RbacMenuEntity> findAllMenus();

    RbacMenuEntity findMenuById(Long id);

    int saveMenu(RbacMenuEntity menu);

    int updateMenu(RbacMenuEntity menu);

    int deleteMenuById(Long id);

    /** 子菜单引用计数（删除菜单前校验） */
    int countMenuByParentId(Long parentId);

    /* ===== 关联维护（覆盖式：先删后插，事务内执行） ===== */

    void deleteUserGroupsByUserId(Long userId);

    int saveUserGroups(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

    List<Long> listGroupIdsByUserId(Long userId);

    void deleteGroupRolesByGroupId(Long groupId);

    int saveGroupRoles(@Param("groupId") Long groupId, @Param("roleIds") List<Long> roleIds);

    List<Long> listRoleIdsByGroupId(Long groupId);

    void deleteRolePermsByRoleId(Long roleId);

    int saveRolePerms(@Param("roleId") Long roleId, @Param("permIds") List<Long> permIds);

    List<Long> listPermIdsByRoleId(Long roleId);

    void deletePermResourcesByPermId(Long permId);

    int savePermResources(@Param("permId") Long permId, @Param("resourceIds") List<Long> resourceIds);

    List<Long> listResourceIdsByPermId(Long permId);

    /* ===== 授权查询 ===== */

    /**
     * 用户可访问资源（用户 → 组 → 角色 → 权限 → 资源，仅取启用链路）
     */
    List<RbacResourceEntity> listResourcesOfUser(Long userId);
}
