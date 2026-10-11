package com.example.fcmrbacmicroservice.service.impl;

import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmrbacmicroservice.mapper.RbacMapper;
import com.example.fcmspi.model.RbacGroupEntity;
import com.example.fcmspi.model.RbacMenuEntity;
import com.example.fcmspi.model.RbacPermEntity;
import com.example.fcmspi.model.RbacResourceEntity;
import com.example.fcmspi.model.RbacRoleEntity;
import com.example.fcmspi.service.RbacService;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RbacServiceImpl implements RbacService {

    /** 资源类型：接口（URL ant 模式，如 /api/order/**） */
    private static final String TYPE_API = "接口";
    /** 资源类型：按钮（如 good:add） */
    private static final String TYPE_BUTTON = "按钮";

    private final RbacMapper rbacMapper;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    public RbacServiceImpl(RbacMapper rbacMapper) {
        this.rbacMapper = rbacMapper;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
    }

    /* ==================== 用户组 ==================== */

    @Override
    public List<RbacGroupEntity> listGroups() {
        return rbacMapper.findAllGroups();
    }

    @Override
    public RbacGroupEntity getGroupById(Long id) {
        return rbacMapper.findGroupById(id);
    }

    @Override
    public int insertGroup(RbacGroupEntity group) {
        if (group.getEnabled() == null) {
            group.setEnabled(true);
        }
        String now = now();
        group.setCreatedTime(now);
        group.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        return rbacMapper.saveGroup(group);
    }

    @Override
    public int updateGroup(RbacGroupEntity group) {
        group.setUpdatedTime(now());
        return rbacMapper.updateGroup(group);
    }

    @Override
    public int deleteGroup(Long id) {
        if (rbacMapper.countUserGroupByGroupId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该组已关联用户，无法删除");
        }
        if (rbacMapper.countGroupRoleByGroupId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该组已分配角色，无法删除");
        }
        return rbacMapper.deleteGroupById(id);
    }

    /* ==================== 角色 ==================== */

    @Override
    public List<RbacRoleEntity> listRoles() {
        return rbacMapper.findAllRoles();
    }

    @Override
    public RbacRoleEntity getRoleById(Long id) {
        return rbacMapper.findRoleById(id);
    }

    @Override
    public int insertRole(RbacRoleEntity role) {
        if (role.getEnabled() == null) {
            role.setEnabled(true);
        }
        String now = now();
        role.setCreatedTime(now);
        role.setUpdatedTime(now);
        return rbacMapper.saveRole(role);
    }

    @Override
    public int updateRole(RbacRoleEntity role) {
        role.setUpdatedTime(now());
        return rbacMapper.updateRole(role);
    }

    @Override
    public int deleteRole(Long id) {
        if (rbacMapper.countGroupRoleByRoleId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该角色已被组引用，无法删除");
        }
        if (rbacMapper.countRolePermByRoleId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该角色已分配权限，无法删除");
        }
        return rbacMapper.deleteRoleById(id);
    }

    /* ==================== 权限 ==================== */

    @Override
    public List<RbacPermEntity> listPerms() {
        return rbacMapper.findAllPerms();
    }

    @Override
    public RbacPermEntity getPermById(Long id) {
        return rbacMapper.findPermById(id);
    }

    @Override
    public int insertPerm(RbacPermEntity perm) {
        if (perm.getEnabled() == null) {
            perm.setEnabled(true);
        }
        String now = now();
        perm.setCreatedTime(now);
        perm.setUpdatedTime(now);
        return rbacMapper.savePerm(perm);
    }

    @Override
    public int updatePerm(RbacPermEntity perm) {
        perm.setUpdatedTime(now());
        return rbacMapper.updatePerm(perm);
    }

    @Override
    public int deletePerm(Long id) {
        if (rbacMapper.countRolePermByPermId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该权限已被角色引用，无法删除");
        }
        if (rbacMapper.countPermResourceByPermId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该权限已分配资源，无法删除");
        }
        return rbacMapper.deletePermById(id);
    }

    /* ==================== 资源 ==================== */

    @Override
    public List<RbacResourceEntity> listResources(String type) {
        return rbacMapper.findAllResources(type);
    }

    @Override
    public RbacResourceEntity getResourceById(Long id) {
        return rbacMapper.findResourceById(id);
    }

    @Override
    public int insertResource(RbacResourceEntity resource) {
        String now = now();
        resource.setCreatedTime(now);
        resource.setUpdatedTime(now);
        return rbacMapper.saveResource(resource);
    }

    @Override
    public int updateResource(RbacResourceEntity resource) {
        resource.setUpdatedTime(now());
        return rbacMapper.updateResource(resource);
    }

    @Override
    public int deleteResource(Long id) {
        if (rbacMapper.countPermResourceByResourceId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该资源已被权限引用，无法删除");
        }
        if (rbacMapper.countMenuByResourceId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "该资源已被菜单引用，无法删除");
        }
        return rbacMapper.deleteResourceById(id);
    }

    /* ==================== 菜单 ==================== */

    @Override
    public List<RbacMenuEntity> menuTree() {
        return buildTree(rbacMapper.findAllMenus(), 0L);
    }

    /** 内存递归组装菜单树（根为 parent_id=0，children 为 transient 字段） */
    private List<RbacMenuEntity> buildTree(List<RbacMenuEntity> all, Long parentId) {
        List<RbacMenuEntity> nodes = new ArrayList<>();
        for (RbacMenuEntity menu : all) {
            if (Objects.equals(parentId, menu.getParentId())) {
                menu.setChildren(buildTree(all, menu.getId()));
                nodes.add(menu);
            }
        }
        return nodes;
    }

    @Override
    public RbacMenuEntity getMenuById(Long id) {
        return rbacMapper.findMenuById(id);
    }

    @Override
    public int insertMenu(RbacMenuEntity menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        String now = now();
        menu.setCreatedTime(now);
        menu.setUpdatedTime(now);
        return rbacMapper.saveMenu(menu);
    }

    @Override
    public int updateMenu(RbacMenuEntity menu) {
        menu.setUpdatedTime(now());
        return rbacMapper.updateMenu(menu);
    }

    @Override
    public int deleteMenu(Long id) {
        if (rbacMapper.countMenuByParentId(id) > 0) {
            throw new BizException(ResponseCode.BAD_REQUEST, "存在子菜单，无法删除");
        }
        return rbacMapper.deleteMenuById(id);
    }

    /* ==================== 关联维护（覆盖式，事务内先删后插） ==================== */

    @Override
    @Transactional
    public int assignGroupsToUser(Long userId, List<Long> groupIds) {
        rbacMapper.deleteUserGroupsByUserId(userId);
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return rbacMapper.saveUserGroups(userId, groupIds);
    }

    @Override
    public List<Long> listGroupIdsByUserId(Long userId) {
        return rbacMapper.listGroupIdsByUserId(userId);
    }

    @Override
    @Transactional
    public int clearGroupsOfUser(Long userId) {
        List<Long> groupIds = rbacMapper.listGroupIdsByUserId(userId);
        rbacMapper.deleteUserGroupsByUserId(userId);
        return groupIds.size();
    }

    @Override
    @Transactional
    public int assignRolesToGroup(Long groupId, List<Long> roleIds) {
        rbacMapper.deleteGroupRolesByGroupId(groupId);
        if (roleIds == null || roleIds.isEmpty()) {
            return 0;
        }
        return rbacMapper.saveGroupRoles(groupId, roleIds);
    }

    @Override
    public List<Long> listRoleIdsByGroupId(Long groupId) {
        return rbacMapper.listRoleIdsByGroupId(groupId);
    }

    @Override
    @Transactional
    public int assignPermsToRole(Long roleId, List<Long> permIds) {
        rbacMapper.deleteRolePermsByRoleId(roleId);
        if (permIds == null || permIds.isEmpty()) {
            return 0;
        }
        return rbacMapper.saveRolePerms(roleId, permIds);
    }

    @Override
    public List<Long> listPermIdsByRoleId(Long roleId) {
        return rbacMapper.listPermIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public int assignResourcesToPerm(Long permId, List<Long> resourceIds) {
        rbacMapper.deletePermResourcesByPermId(permId);
        if (resourceIds == null || resourceIds.isEmpty()) {
            return 0;
        }
        return rbacMapper.savePermResources(permId, resourceIds);
    }

    @Override
    public List<Long> listResourceIdsByPermId(Long permId) {
        return rbacMapper.listResourceIdsByPermId(permId);
    }

    /* ==================== 授权判定 / 动态菜单 ==================== */

    @Override
    public boolean checkPermission(Long userId, String path, String method) {
        if (userId == null || path == null || path.isEmpty()) {
            return false;
        }
        for (RbacResourceEntity resource : rbacMapper.listResourcesOfUser(userId)) {
            if (!TYPE_API.equals(resource.getType())) {
                continue;
            }
            // 接口资源 value 存 ant 风格路径（如 /api/order/**）；method 维度暂未建模，预留
            PathPattern pattern = pathPatternParser.parse(resource.getValue());
            if (pattern.matches(PathContainer.parsePath(path))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<RbacMenuEntity> menusOfUser(Long userId) {
        Set<Long> resourceIds = rbacMapper.listResourcesOfUser(userId).stream()
                .map(RbacResourceEntity::getId)
                .collect(Collectors.toSet());
        return filterTree(rbacMapper.findAllMenus(), 0L, resourceIds);
    }

    /** 按用户资源过滤菜单树：挂钩资源的节点须被授权；空目录节点收敛 */
    private List<RbacMenuEntity> filterTree(List<RbacMenuEntity> all, Long parentId, Set<Long> resourceIds) {
        List<RbacMenuEntity> nodes = new ArrayList<>();
        for (RbacMenuEntity menu : all) {
            if (!Objects.equals(parentId, menu.getParentId())) {
                continue;
            }
            if (menu.getResourceId() != null && !resourceIds.contains(menu.getResourceId())) {
                continue;
            }
            List<RbacMenuEntity> children = filterTree(all, menu.getId(), resourceIds);
            if (menu.getResourceId() == null && menu.getUrl() == null && children.isEmpty()) {
                continue;
            }
            menu.setChildren(children);
            nodes.add(menu);
        }
        return nodes;
    }

    @Override
    public List<String> listResourceValuesOfUser(Long userId) {
        return rbacMapper.listResourcesOfUser(userId).stream()
                .map(RbacResourceEntity::getValue)
                .collect(Collectors.toList());
    }
}
