-- =====================================================================
-- 修复管理端 RBAC 授权数据：将 9 张 t_rbac_* 表替换为「测试数据.sql」14~22 节定义
-- 背景：当前库中为旧版种子数据（资源为 /api/order/** 单数路径且缺失大部分模块），
--       导致网关 RBAC 判定失败，管理端所有写操作 403【无权限访问该资源】
-- 说明：已于 2026-09-14 执行并验证（admin 写路径全部放行，发货/订单列表端到端通过）
-- =====================================================================
USE `flashchoice-mart`;

-- 先清空关联表与主表（无外键约束，顺序仅为语义清晰）
DELETE FROM `t_rbac_user_group`;
DELETE FROM `t_rbac_group_role`;
DELETE FROM `t_rbac_role_perm`;
DELETE FROM `t_rbac_perm_resource`;
DELETE FROM `t_rbac_menu`;
DELETE FROM `t_rbac_resource`;
DELETE FROM `t_rbac_perm`;
DELETE FROM `t_rbac_role`;
DELETE FROM `t_rbac_group`;

-- 14. 用户组
INSERT INTO `t_rbac_group` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '管理员组', 1, '拥有所有权限', NOW(), 1, NOW(), 1),
(2, '运营组', 1, '负责商品和订单管理', NOW(), 1, NOW(), 1);

-- 15. 角色
INSERT INTO `t_rbac_role` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'ROLE_ADMIN', 1, '管理员角色', NOW(), 1, NOW(), 1),
(2, 'ROLE_MANAGER', 1, '运营经理角色', NOW(), 1, NOW(), 1);

-- 16. 权限（按后台模块划分；系统管理覆盖用户/用户组/角色/权限/资源/菜单）
INSERT INTO `t_rbac_perm` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'PERM_BRAND_MANAGE', 1, '品牌管理', NOW(), 1, NOW(), 1),
(2, 'PERM_CATEGORY_MANAGE', 1, '分类管理', NOW(), 1, NOW(), 1),
(3, 'PERM_GOOD_MANAGE', 1, '商品管理（含按钮）', NOW(), 1, NOW(), 1),
(4, 'PERM_ORDER_MANAGE', 1, '订单管理', NOW(), 1, NOW(), 1),
(5, 'PERM_MEMBER_MANAGE', 1, '会员管理', NOW(), 1, NOW(), 1),
(6, 'PERM_SECKILL_MANAGE', 1, '秒杀管理', NOW(), 1, NOW(), 1),
(7, 'PERM_SYSTEM_MANAGE', 1, '系统管理（用户/用户组/角色/权限/资源/菜单）', NOW(), 1, NOW(), 1),
(8, 'PERM_REGION_MANAGE', 1, '地区管理', NOW(), 1, NOW(), 1),
(9, 'PERM_UPLOAD', 1, '文件上传', NOW(), 1, NOW(), 1);

-- 17. 资源（按钮 + 接口；接口 value 为 ant 路径，与 RbacService.checkPermission 的匹配规则一致，对应真实控制器路径）
INSERT INTO `t_rbac_resource` (`id`, `name`, `type`, `value`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '新增商品', '按钮', 'good:add', '新增商品按钮', NOW(), 1, NOW(), 1),
(2, '编辑商品', '按钮', 'good:edit', '编辑商品按钮', NOW(), 1, NOW(), 1),
(3, '删除商品', '按钮', 'good:delete', '删除商品按钮', NOW(), 1, NOW(), 1),
(4, '品牌管理', '接口', '/api/brands/**', '品牌管理接口', NOW(), 1, NOW(), 1),
(5, '分类管理', '接口', '/api/categories/**', '分类管理接口', NOW(), 1, NOW(), 1),
(6, '商品管理', '接口', '/api/goods/**', '商品管理接口', NOW(), 1, NOW(), 1),
(7, '订单管理', '接口', '/api/orders/**', '订单管理接口', NOW(), 1, NOW(), 1),
(8, '会员管理', '接口', '/api/members/**', '会员管理接口', NOW(), 1, NOW(), 1),
(9, '秒杀管理', '接口', '/api/seckills/**', '秒杀管理接口', NOW(), 1, NOW(), 1),
(10, '用户管理', '接口', '/api/users/**', '后台用户管理接口', NOW(), 1, NOW(), 1),
(11, '用户组管理', '接口', '/api/groups/**', '用户组管理接口', NOW(), 1, NOW(), 1),
(12, '角色管理', '接口', '/api/roles/**', '角色管理接口', NOW(), 1, NOW(), 1),
(13, '权限管理', '接口', '/api/perms/**', '权限管理接口', NOW(), 1, NOW(), 1),
(14, '资源管理', '接口', '/api/resources/**', '资源管理接口', NOW(), 1, NOW(), 1),
(15, '菜单管理', '接口', '/api/menus/**', '菜单管理接口', NOW(), 1, NOW(), 1),
(16, '用户组分配', '接口', '/api/rbac/**', '用户-用户组分配接口', NOW(), 1, NOW(), 1),
(17, '地区管理', '接口', '/api/regions/**', '地区管理接口', NOW(), 1, NOW(), 1),
(18, '文件上传', '接口', '/upload/**', '商品图/头像上传接口', NOW(), 1, NOW(), 1);

-- 18. 菜单（resource_id 挂钩资源，/api/menus 按当前用户资源动态过滤返回）
INSERT INTO `t_rbac_menu` (`id`, `parent_id`, `resource_id`, `name`, `icon`, `url`, `sort`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 0, 4, '品牌管理', 'Collection', '/brands', 1, '品牌管理菜单', NOW(), 1, NOW(), 1),
(2, 0, 5, '分类管理', 'Grid', '/categories', 2, '分类管理菜单', NOW(), 1, NOW(), 1),
(3, 0, 6, '商品管理', 'Goods', '/goods', 3, '商品管理菜单', NOW(), 1, NOW(), 1),
(4, 0, 7, '订单管理', 'List', '/orders', 4, '订单管理菜单', NOW(), 1, NOW(), 1),
(5, 0, 8, '会员管理', 'User', '/members', 5, '会员管理菜单', NOW(), 1, NOW(), 1),
(6, 0, 9, '秒杀管理', 'Clock', '/seckills', 6, '秒杀管理菜单', NOW(), 1, NOW(), 1),
(7, 0, NULL, '系统管理', 'Setting', NULL, 7, '系统管理目录（无资源挂钩，按子菜单过滤）', NOW(), 1, NOW(), 1),
(8, 7, 10, '用户管理', 'UserFilled', '/system/users', 1, '后台用户管理', NOW(), 1, NOW(), 1),
(9, 7, 11, '用户组管理', 'OfficeBuilding', '/system/groups', 2, '用户组管理', NOW(), 1, NOW(), 1),
(10, 7, 12, '角色管理', 'Avatar', '/system/roles', 3, '角色管理', NOW(), 1, NOW(), 1),
(11, 7, 13, '权限管理', 'Key', '/system/perms', 4, '权限管理', NOW(), 1, NOW(), 1),
(12, 7, 14, '资源管理', 'Box', '/system/resources', 5, '资源管理', NOW(), 1, NOW(), 1),
(13, 7, 15, '菜单管理', 'Menu', '/system/menus', 6, '菜单管理', NOW(), 1, NOW(), 1),
(14, 0, 17, '地区管理', 'Location', '/regions', 8, '地区管理菜单', NOW(), 1, NOW(), 1);

-- 19. 用户-组关联
INSERT INTO `t_rbac_user_group` (`user_id`, `group_id`)
VALUES
(1, 1),
(2, 2);

-- 20. 组-角色关联
INSERT INTO `t_rbac_group_role` (`group_id`, `role_id`)
VALUES
(1, 1),
(2, 2);

-- 21. 角色-权限关联（管理员全量；运营经理：品牌/分类/商品/订单/会员/秒杀/上传）
INSERT INTO `t_rbac_role_perm` (`role_id`, `perm_id`)
VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 9);

-- 22. 权限-资源关联
INSERT INTO `t_rbac_perm_resource` (`resource_id`, `perm_id`)
VALUES
(1, 3), (2, 3), (3, 3),
(4, 1), (5, 2), (6, 3),
(7, 4), (8, 5), (9, 6),
(10, 7), (11, 7), (12, 7), (13, 7), (14, 7), (15, 7), (16, 7),
(17, 8), (18, 9);
