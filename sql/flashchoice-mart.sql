-- =====================================================================
-- FlashChoice-Mart 极速优选商城 数据库初始化脚本
-- 目标数据库：flashchoice-mart（已创建，字符集 utf8mb4）
-- 依据：《基于SpringCloud的B2C微服务秒杀商城-详细设计说明书》V1.0.2 第3章
-- 数据对象：24 张业务表 + 1 张认证支撑表（t_refresh_token，JWT 双 Token 认证用）
-- 更新记录：V1.1 新增 24.collect 商品收藏表（会员收藏功能，对应 FCM-collect-microservice）
--          V1.2 新增 25.notice 会员通知表（收藏商品降价/进入秒杀站内信，随 FCM-collect-microservice）
-- 说明：order / user 为保留字，SQL 与 MyBatis XML 中均需使用反引号
-- =====================================================================

USE `flashchoice-mart`;

-- ---------------------------------------------------------------------
-- 1. brand 商品品牌
-- ---------------------------------------------------------------------
CREATE TABLE `brand` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(30)  NOT NULL                COMMENT '品牌名称',
    `company`      VARCHAR(255) DEFAULT NULL            COMMENT '公司名称',
    `logo`         VARCHAR(255) DEFAULT NULL            COMMENT 'Logo URL',
    `site`         VARCHAR(255) DEFAULT NULL            COMMENT '网址',
    `description`  VARCHAR(1024) DEFAULT NULL           COMMENT '简介',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品品牌';

-- ---------------------------------------------------------------------
-- 2. category 商品分类（parent_id 自关联树）
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`    BIGINT        NOT NULL DEFAULT 0     COMMENT '父分类ID（0为根）',
    `name`         VARCHAR(50)   NOT NULL               COMMENT '名称',
    `title`        VARCHAR(100)  DEFAULT NULL           COMMENT '标题',
    `tag`          VARCHAR(100)  DEFAULT NULL           COMMENT '标签',
    `icon`         VARCHAR(255)  DEFAULT NULL           COMMENT '图标',
    `summary`      VARCHAR(500)  DEFAULT NULL           COMMENT '摘要',
    `sort`         INT           NOT NULL DEFAULT 0     COMMENT '排序',
    `description`  VARCHAR(1024) DEFAULT NULL           COMMENT '简介',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT        DEFAULT NULL           COMMENT '创建人ID',
    `updated_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT        DEFAULT NULL           COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_category_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品分类';

-- ---------------------------------------------------------------------
-- 3. good 商品SPU
-- ---------------------------------------------------------------------
CREATE TABLE `good` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_no`       VARCHAR(30)   NOT NULL                COMMENT '货号',
    `name`         VARCHAR(100)  NOT NULL                COMMENT '名称',
    `alias`        VARCHAR(100)  DEFAULT NULL            COMMENT '别名',
    `summary`      VARCHAR(500)  DEFAULT NULL            COMMENT '摘要',
    `category_id`  BIGINT        DEFAULT NULL            COMMENT '分类ID',
    `brand_id`     BIGINT        DEFAULT NULL            COMMENT '品牌ID',
    `mark_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '标价',
    `price`        DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '实价',
    `qty`          INT           NOT NULL DEFAULT 0      COMMENT '库存',
    `pic`          VARCHAR(255)  DEFAULT NULL            COMMENT '主图',
    `pic2`         VARCHAR(255)  DEFAULT NULL            COMMENT '次图',
    `detail_pics`  VARCHAR(2000) DEFAULT NULL            COMMENT '详情图URL集合',
    `detail`       TEXT                                  COMMENT '富文本详情',
    `is_take_down` TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否下架（0上架/1下架）',
    `is_hot`       TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否热销',
    `is_del`       TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除（0正常/1删除）',
    `is_seckill`   TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否秒杀商品',
    `description`  VARCHAR(1024) DEFAULT NULL            COMMENT '备注',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT        DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT        DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_good_spu_no` (`spu_no`),
    KEY `idx_good_category_id` (`category_id`),
    KEY `idx_good_brand_id` (`brand_id`),
    KEY `idx_good_name` (`name`),
    KEY `idx_good_hot_del` (`is_hot`, `is_del`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品SPU';

-- ---------------------------------------------------------------------
-- 4. good_detail_pics 商品详情图片
-- ---------------------------------------------------------------------
CREATE TABLE `good_detail_pics` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `good_id`     BIGINT       NOT NULL                COMMENT '商品ID',
    `url`         VARCHAR(255) NOT NULL                COMMENT '图片URL',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    PRIMARY KEY (`id`),
    KEY `idx_good_detail_pics_good_id` (`good_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品详情图片';

-- ---------------------------------------------------------------------
-- 5. cart 购物车
-- ---------------------------------------------------------------------
CREATE TABLE `cart` (
    `id`        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` BIGINT NOT NULL                COMMENT '会员ID',
    `good_id`   BIGINT NOT NULL                COMMENT '商品ID',
    `qty`       INT    NOT NULL DEFAULT 1      COMMENT '数量',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_member_good` (`member_id`, `good_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '购物车';

-- ---------------------------------------------------------------------
-- 6. member 会员
-- ---------------------------------------------------------------------
CREATE TABLE `member` (
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account`               VARCHAR(30)   NOT NULL                COMMENT '登录账号',
    `password`              VARCHAR(100)  NOT NULL                COMMENT '密码（BCrypt）',
    `enabled`               TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用（0禁用/1启用）',
    `name`                  VARCHAR(30)   DEFAULT NULL            COMMENT '姓名',
    `pinyin`                VARCHAR(100)  DEFAULT NULL            COMMENT '姓名拼音（带音调）',
    `pinyin_untoned`        VARCHAR(100)  DEFAULT NULL            COMMENT '姓名拼音（无音调）',
    `sex`                   VARCHAR(10)   DEFAULT NULL            COMMENT '性别',
    `birthday`              DATE          DEFAULT NULL            COMMENT '生日',
    `height`                TINYINT       DEFAULT NULL            COMMENT '身高(cm)',
    `weight`                DECIMAL(5,2)  DEFAULT NULL            COMMENT '体重(kg)',
    `iq`                    TINYINT       DEFAULT NULL            COMMENT 'IQ',
    `qq`                    VARCHAR(20)   DEFAULT NULL            COMMENT 'QQ',
    `wechat`                VARCHAR(30)   DEFAULT NULL            COMMENT '微信',
    `phone`                 VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
    `email`                 VARCHAR(50)   DEFAULT NULL            COMMENT '邮箱',
    `native_place_id`       BIGINT        DEFAULT NULL            COMMENT '籍贯地区ID',
    `card_id`               VARCHAR(30)   DEFAULT NULL            COMMENT '证件号',
    `wedlock`               VARCHAR(10)   DEFAULT NULL            COMMENT '婚姻状况',
    `political_orientation` VARCHAR(20)   DEFAULT NULL            COMMENT '政治面貌',
    `address_id`            BIGINT        DEFAULT NULL            COMMENT '现住地区ID',
    `address_detail`        VARCHAR(255)  DEFAULT NULL            COMMENT '详细地址',
    `race`                  VARCHAR(20)   DEFAULT NULL            COMMENT '民族',
    `religion`              VARCHAR(20)   DEFAULT NULL            COMMENT '宗教',
    `nationality`           VARCHAR(20)   DEFAULT NULL            COMMENT '国籍',
    `portrait`              VARCHAR(255)  DEFAULT NULL            COMMENT '头像',
    `description`           TEXT                                  COMMENT '简介',
    `extra_info`            JSON                                  COMMENT '扩展信息',
    `last_login_time`       DATETIME      DEFAULT NULL            COMMENT '最近登录时间',
    `last_login_ip`         VARCHAR(50)   DEFAULT NULL            COMMENT '最近登录IP',
    `version`               INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
    `created_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`            BIGINT        DEFAULT NULL            COMMENT '创建人ID',
    `updated_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`            BIGINT        DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_account` (`account`),
    KEY `idx_member_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会员';

-- ---------------------------------------------------------------------
-- 7. member_address 会员收货地址
-- ---------------------------------------------------------------------
CREATE TABLE `member_address` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_account` VARCHAR(30)  NOT NULL                COMMENT '会员账号',
    `receiver`       VARCHAR(30)  NOT NULL                COMMENT '收货人',
    `phone`          VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `addr_id`        BIGINT       DEFAULT NULL            COMMENT '所在地区ID（省市区）',
    `addr_detail`    VARCHAR(255) DEFAULT NULL            COMMENT '详细地址',
    `is_default`     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否默认（0否/1是）',
    PRIMARY KEY (`id`),
    KEY `idx_member_address_account` (`member_account`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会员收货地址';

-- ---------------------------------------------------------------------
-- 8. order 订单（order为保留字，须使用反引号）
-- ---------------------------------------------------------------------
CREATE TABLE `order` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`            VARCHAR(32)   NOT NULL                COMMENT '订单号（雪花ID）',
    `seckill_no`          VARCHAR(32)   DEFAULT NULL            COMMENT '秒杀流水号（秒杀订单填写）',
    `member_account`      VARCHAR(30)   NOT NULL                COMMENT '下单会员账号',
    `total_pay`           DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '应付总额',
    `pay_type`            VARCHAR(20)   DEFAULT NULL            COMMENT '支付方式（模拟支付）',
    `alipay_trade_no`     VARCHAR(64)   DEFAULT NULL            COMMENT '支付交易号（模拟支付流水号）',
    `checkout_time`       DATETIME      DEFAULT NULL            COMMENT '下单时间',
    `pay_time`            DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `ship_time`           DATETIME      DEFAULT NULL            COMMENT '发货时间',
    `accept_time`         DATETIME      DEFAULT NULL            COMMENT '收货时间',
    `status`              VARCHAR(20)   NOT NULL DEFAULT '待付款' COMMENT '订单状态（待付款/已支付/待收货/已确认/已取消）',
    `receiver_addr_id`    BIGINT        DEFAULT NULL            COMMENT '收货地址ID（快照来源）',
    `receiver_name`       VARCHAR(30)   DEFAULT NULL            COMMENT '收货人快照',
    `receiver_phone`      VARCHAR(20)   DEFAULT NULL            COMMENT '收货电话快照',
    `receiver_addr_detail` VARCHAR(255) DEFAULT NULL            COMMENT '收货地址快照',
    `order_comment`       VARCHAR(500)  DEFAULT NULL            COMMENT '订单备注',
    `refund_status`       VARCHAR(20)   NOT NULL DEFAULT '无退款' COMMENT '退款状态（无退款/退款中/已退款）',
    `is_del`              TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除（0正常/1删除）',
    `description`         VARCHAR(1024) DEFAULT NULL            COMMENT '备注',
    `created_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`          BIGINT        DEFAULT NULL            COMMENT '创建人ID',
    `updated_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`          BIGINT        DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_order_member_created` (`member_account`, `created_time`),
    KEY `idx_order_status` (`status`),
    UNIQUE KEY `uk_order_seckill_no` (`seckill_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单';

-- ---------------------------------------------------------------------
-- 9. order_item 订单明细
-- ---------------------------------------------------------------------
CREATE TABLE `order_item` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`   BIGINT        NOT NULL                COMMENT '订单ID',
    `good_id`    BIGINT        NOT NULL                COMMENT '商品ID',
    `deal_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '成交价',
    `count`      INT           NOT NULL DEFAULT 1      COMMENT '数量',
    `good_name`  VARCHAR(100)  DEFAULT NULL            COMMENT '商品名称快照',
    `good_pic`   VARCHAR(255)  DEFAULT NULL            COMMENT '商品图片快照',
    `good_desc`  VARCHAR(500)  DEFAULT NULL            COMMENT '商品描述快照',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单明细';

-- ---------------------------------------------------------------------
-- 10. seckill 秒杀活动
-- ---------------------------------------------------------------------
CREATE TABLE `seckill` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)   NOT NULL                COMMENT '活动名称',
    `enabled`      TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用（0禁用/1启用）',
    `start_time`   DATETIME      NOT NULL                COMMENT '开始时间',
    `end_time`     DATETIME      NOT NULL                COMMENT '结束时间',
    `description`  VARCHAR(1024) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT        DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT        DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_seckill_start_time` (`start_time`),
    KEY `idx_seckill_end_time` (`end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀活动';

-- ---------------------------------------------------------------------
-- 11. seckill_good 秒杀活动商品
-- ---------------------------------------------------------------------
CREATE TABLE `seckill_good` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `seckill_id`     BIGINT        NOT NULL                COMMENT '秒杀活动ID',
    `good_id`        BIGINT        NOT NULL                COMMENT '商品ID',
    `seckill_price`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '秒杀价',
    `stock`          INT           NOT NULL DEFAULT 0      COMMENT '秒杀库存',
    `sold`           INT           NOT NULL DEFAULT 0      COMMENT '已售数量',
    `limit_per_user` INT           NOT NULL DEFAULT 1      COMMENT '每人限购数',
    `description`    VARCHAR(1024) DEFAULT NULL            COMMENT '说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_good_seckill_good` (`seckill_id`, `good_id`),
    KEY `idx_seckill_good_seckill_id` (`seckill_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀活动商品';

-- 抢购流水：持久化异步处理结果，消息重投时按 seckill_no 幂等处理。
CREATE TABLE `seckill_grab` (
    `seckill_no` VARCHAR(32) NOT NULL,
    `seckill_good_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `member_account` VARCHAR(30) NOT NULL,
    `addr_id` BIGINT NOT NULL,
    `seckill_price` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `order_no` VARCHAR(32) DEFAULT NULL,
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`seckill_no`),
    KEY `idx_grab_member_good` (`member_id`, `seckill_good_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀抢购流水';

-- ---------------------------------------------------------------------
-- 12. t_china_region 行政区划（省/市/区三级树）
-- ---------------------------------------------------------------------
CREATE TABLE `t_china_region` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '名称',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父级ID（0为根）',
    `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `level`       TINYINT      NOT NULL DEFAULT 1      COMMENT '层级（1省/2市/3区）',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    PRIMARY KEY (`id`),
    KEY `idx_region_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '行政区划';

-- ---------------------------------------------------------------------
-- 13. user 后台用户（user为保留字，须使用反引号）
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`               VARCHAR(30)   NOT NULL                COMMENT '用户名',
    `password`               VARCHAR(100)  NOT NULL                COMMENT '密码（BCrypt）',
    `avatar_url`             VARCHAR(255)  DEFAULT NULL            COMMENT '头像',
    `enabled`                TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用（0禁用/1启用）',
    `status`                 TINYINT       NOT NULL DEFAULT 0      COMMENT '账号状态',
    `locked`                 TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否锁定（0否/1是）',
    `user_expire_time`       DATETIME      DEFAULT NULL            COMMENT '账号过期时间',
    `credential_expire_time` DATETIME      DEFAULT NULL            COMMENT '凭证过期时间',
    `login_times`            INT           NOT NULL DEFAULT 0      COMMENT '登录次数',
    `last_login_time`        DATETIME      DEFAULT NULL            COMMENT '最近登录时间',
    `last_login_ip`          VARCHAR(50)   DEFAULT NULL            COMMENT '最近登录IP',
    `op_mode`                INT           NOT NULL DEFAULT 0      COMMENT '操作模式',
    `description`            VARCHAR(1024) DEFAULT NULL            COMMENT '说明',
    `created_time`           DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`             BIGINT        DEFAULT NULL            COMMENT '创建人ID',
    `updated_time`           DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`             BIGINT        DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '后台用户';

-- ---------------------------------------------------------------------
-- 14. t_rbac_group 用户组
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_group` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)  NOT NULL                COMMENT '用户组名称',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_group_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC用户组';

-- ---------------------------------------------------------------------
-- 15. t_rbac_role 角色
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_role` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)  NOT NULL                COMMENT '角色名称',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_role_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC角色';

-- ---------------------------------------------------------------------
-- 16. t_rbac_perm 权限
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_perm` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)  NOT NULL                COMMENT '权限名称',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_perm_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC权限';

-- ---------------------------------------------------------------------
-- 17. t_rbac_resource 资源（接口/按钮）
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_resource` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)  NOT NULL                COMMENT '资源名称',
    `type`         VARCHAR(20)  NOT NULL                COMMENT '资源类型（接口/按钮）',
    `value`        VARCHAR(255) NOT NULL                COMMENT '资源值（路径或按钮标识）',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_resource_type_value` (`type`, `value`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC资源';

-- ---------------------------------------------------------------------
-- 18. t_rbac_menu 菜单
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_menu` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`    BIGINT       NOT NULL DEFAULT 0      COMMENT '父菜单ID（0为根）',
    `resource_id`  BIGINT       DEFAULT NULL            COMMENT '资源ID（关联t_rbac_resource）',
    `name`         VARCHAR(50)  NOT NULL                COMMENT '菜单名称',
    `icon`         VARCHAR(255) DEFAULT NULL            COMMENT '图标',
    `url`          VARCHAR(255) DEFAULT NULL            COMMENT '菜单URL',
    `sort`         INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`   BIGINT       DEFAULT NULL            COMMENT '创建人ID',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT       DEFAULT NULL            COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_rbac_menu_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC菜单';

-- ---------------------------------------------------------------------
-- 19. t_rbac_user_group 用户-组 关联
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_user_group` (
    `id`       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`  BIGINT NOT NULL                COMMENT '用户ID',
    `group_id` BIGINT NOT NULL                COMMENT '用户组ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_user_group` (`user_id`, `group_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC用户-组关联';

-- ---------------------------------------------------------------------
-- 20. t_rbac_group_role 组-角色 关联
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_group_role` (
    `id`       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL                COMMENT '用户组ID',
    `role_id`  BIGINT NOT NULL                COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_group_role` (`group_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC组-角色关联';

-- ---------------------------------------------------------------------
-- 21. t_rbac_role_perm 角色-权限 关联
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_role_perm` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` BIGINT NOT NULL                COMMENT '角色ID',
    `perm_id` BIGINT NOT NULL                COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_role_perm` (`role_id`, `perm_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC角色-权限关联';

-- ---------------------------------------------------------------------
-- 22. t_rbac_perm_resource 权限-资源 关联
-- ---------------------------------------------------------------------
CREATE TABLE `t_rbac_perm_resource` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT NOT NULL                COMMENT '资源ID',
    `perm_id`     BIGINT NOT NULL                COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rbac_perm_resource` (`resource_id`, `perm_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RBAC权限-资源关联';

-- ---------------------------------------------------------------------
-- 23. t_refresh_token 刷新令牌（JWT 双 Token 认证支撑表）
--     说明：由 user-api（audience=admin）与 member-api（audience=member）共用；
--           Refresh Token 一次性轮换，刷新/登出时删除旧记录实现主动失效；
--           超期记录由各签发服务启动后定时清理
-- ---------------------------------------------------------------------
CREATE TABLE `t_refresh_token` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `audience`    VARCHAR(16)  NOT NULL                COMMENT '令牌类型（admin 后台用户 / member 会员）',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID（按 audience 关联 user/member 主键）',
    `token`       VARCHAR(600) NOT NULL                COMMENT '刷新令牌（JWT 串）',
    `expire_time` DATETIME     NOT NULL                COMMENT '过期时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_token` (`token`),
    KEY `idx_audience_user` (`audience`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '刷新令牌';

-- ---------------------------------------------------------------------
-- 24. collect 商品收藏（member_id + good_id 唯一，重复收藏幂等）
-- ---------------------------------------------------------------------
CREATE TABLE `collect` (
    `id`        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` BIGINT NOT NULL                COMMENT '会员ID',
    `good_id`   BIGINT NOT NULL                COMMENT '商品ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collect_member_good` (`member_id`, `good_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品收藏';

-- ---------------------------------------------------------------------
-- 25. notice 会员通知（站内信：收藏商品降价 / 进入秒杀触达）
-- ---------------------------------------------------------------------
CREATE TABLE `notice` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id`    BIGINT       NOT NULL                COMMENT '接收会员ID',
    `type`         TINYINT      NOT NULL                COMMENT '通知类型：1降价 2进入秒杀',
    `good_id`      BIGINT       NOT NULL                COMMENT '关联商品ID',
    `title`        VARCHAR(100) NOT NULL                COMMENT '标题',
    `content`      VARCHAR(500) DEFAULT NULL            COMMENT '内容',
    `is_read`      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否已读（0未读/1已读）',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notice_member_read` (`member_id`, `is_read`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会员通知（站内信）';
