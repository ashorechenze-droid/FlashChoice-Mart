# 基于 Spring Cloud 的 B2C 微服务秒杀商城
# 详细设计说明书（DDD）

| 项目名称 | 基于 Spring Cloud 的 B2C 微服务秒杀商城 |
| --- | --- |
| 文档版本 | V1.0.2 |
| 文档状态 | 评审稿 |
| 编写日期 | 2026-09-02 |
| 依据文档 | 《基于 Spring Cloud 的 B2C 微服务秒杀商城 需求规格说明书》V1.0.2 |
| 密级 | 内部 |

---

# 1 引言

## 1.1 编写目的

本文档在需求规格说明书（SRS V1.0.2）基础上，对「基于 Spring Cloud 的 B2C 微服务秒杀商城」进行详细设计，明确系统架构、技术选型、数据库结构、接口契约、模块职责、关键业务流程与非功能设计，作为编码实现与测试的依据。

## 1.2 设计范围

本文档覆盖 SRS 定义的全部功能范围：

- PC 商城前端（会员端）与后台管理系统（管理端）两套前端；
- 后端微服务集群（Spring Cloud + Spring Cloud Alibaba）；
- 商品中心、交易中心、会员中心、秒杀中心、权限中心、基础数据、文件服务、系统支撑等业务域。

明确的设计约束（承接 SRS 2.5）：

- 持久层使用 MyBatis，不采用 MyBatis-Plus；
- 消息队列采用 RabbitMQ；
- 支付与退款以模拟方式实现，不接入真实支付渠道；
- 系统不包含商品 SKU/规格体系（商品以单品 SPU 管理）。

## 1.3 术语与缩略语

| 术语 | 说明 |
| --- | --- |
| SRS | 需求规格说明书 |
| SPI | 服务提供接口模块（模型与业务接口定义） |
| PageHelper | MyBatis 分页插件 |
| JWT | 登录令牌 |
| 模拟支付 | 不接入真实支付渠道，由系统模拟收银台完成支付确认 |
| 延时消息 | 通过 RabbitMQ 的 TTL + 死信队列实现的延迟投递消息 |

## 1.4 参考资料

| 编号 | 名称 | 用途 |
| --- | --- | --- |
| [1] | 需求规格说明书 V1.0.2 | 设计依据 |
| [2] | shoplook2026-parent（工程布局参考） | 仅参考模块划分、分层、命名与技术约定 |
| [3] | shoplook2026-0831.sql（数据模型参考） | 数据对象与字段结构参考 |
| [4] | Spring Boot / Spring Cloud / Spring Cloud Alibaba 官方文档 | 技术资料 |

说明：参考材料仅用于梳理布局与数据结构，本设计按目标系统独立编写，不依赖任何既有实现。

---

# 2 总体设计

## 2.1 设计原则

1. 按业务域拆分微服务，服务可独立部署、独立扩展；
2. 模型与业务接口统一定义在 spi 模块，实现分散在各 api 服务，避免循环依赖；
3. 通用能力（响应结构、分页、审计、实体约定）下沉到公共模块统一提供；
4. 服务间禁止直接访问数据库，一律通过 Feign 远程调用；
5. 前端与后端完全分离，仅通过网关暴露的接口交互。

## 2.2 系统架构

### 2.2.1 逻辑架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 表现层   PC 商城前端（Vue3）   后台管理系统（Vue3 + Element Plus）      │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTPS
┌───────────────────────────────▼─────────────────────────────────────────┐
│ 接入层   Nginx（静态资源 + 反代） → Spring Cloud Gateway（WebFlux）     │
│         路由 / JWT 校验 / RBAC 权限判定 / 限流 / CORS                   │
└───────────────────────────────┬─────────────────────────────────────────┘
┌───────────────────────────────▼─────────────────────────────────────────┐
│ 服务层   商品中心/交易中心/会员中心/秒杀中心/权限中心/基础数据/文件服务 │
└───────────────────────────────┬─────────────────────────────────────────┘
┌───────────────────────────────▼─────────────────────────────────────────┐
│ 数据层   MySQL    Redis    RabbitMQ    文件存储                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2.2 部署视图

- 前端：两套 Vue3 应用构建产物由 Nginx 静态托管，Nginx 将 /api/** 与 /upload/** 反代到网关；
- 网关：唯一外部入口（:8888），注册到 Nacos；
- 业务服务：各 api 服务注册到 Nacos，经网关路由；服务间通过 Feign 调用；
- 基础设施：Nacos（注册/配置中心）、MySQL、Redis、RabbitMQ 独立部署，支持容器化。

## 2.3 技术选型

| 项 | 选型 | 版本 |
| --- | --- | --- |
| 语言/运行时 | Java | 25.0.4 |
| 核心框架 | Spring Boot | 4.1.1 |
| 微服务框架 | Spring Cloud | 2025.1.3 |
| 微服务组件 | Spring Cloud Alibaba | 2025.1.0.0 |
| 注册/配置中心 | Nacos Discovery / Config | 3.x |
| 网关 | Spring Cloud Gateway（WebFlux） | 由 Spring Cloud 管理 |
| 持久层 | MyBatis + PageHelper + MySQL Connector/J + HikariCP | — |
| 服务调用 | OpenFeign | — |
| 缓存 | Redis（Spring Data Redis） | 8.x |
| 消息队列 | RabbitMQ（Spring AMQP） | 3.x |
| 认证 | JWT（jjwt）+ 网关过滤器 | — |
| 工具库 | Lombok、Hutool | — |
| 后台管理端 | Vue3 + Element Plus + JavaScript + Axios + Pinia + Vue Router + Vite | — |
| PC 商城前端 | Vue3 + JavaScript + Axios + Pinia + Vue Router + Vite | — |

## 2.4 微服务划分与端口规划

| 服务名 | 端口 | 职责 | 状态 |
| --- | --- | --- | --- |
| shoplook2026-gateway | 8888 | 统一网关（WebFlux）：路由、JWT 校验、RBAC 判定、限流、CORS | 本期建设 |
| shoplook2026-brand-api | 10010 | 品牌管理 | 本期建设 |
| shoplook2026-category-api | 10011 | 分类管理（树） | 本期建设 |
| shoplook2026-good-api | 10012 | 商品管理（含品牌/分类关联） | 本期建设 |
| shoplook2026-member-api | 10013 | 会员管理、会员登录 | 本期建设 |
| shoplook2026-member-address-api | 10014 | 会员收货地址 | 本期建设 |
| shoplook2026-region-api | 10015 | 行政区划 | 本期建设 |
| shoplook2026-cart-api | 10016 | 购物车 | 本期建设 |
| shoplook2026-order-api | 10017 | 订单与订单明细、模拟支付、退款 | 本期建设 |
| shoplook2026-user-api | 10018 | 后台用户、后台登录 | 本期建设 |
| shoplook2026-rbac-api | 10019 | RBAC 权限、动态菜单、权限判定 | 本期建设 |
| shoplook2026-upload-api | 10020 | 文件上传与静态资源 | 本期建设 |
| shoplook2026-seckill-api | 10021 | 秒杀活动/秒杀商品、抢购 | 本期建设 |

说明：

- 服务名即 Maven 模块名（shoplook2026-xxx-api），注册名（spring.application.name）与服务名一致；
- 网关仅采用 WebFlux 版本（WebMVC 版仅作演示，正式设计不采用）；
- 秒杀服务为按 SRS 新增的业务服务。

## 2.5 工程结构

```
shoplook2026-parent（pom 父工程）
├── shoplook2026-common        # 公共能力：JsonResp、TypedJsonResp、PaginateInfo、AuditEntity、Identity、通用工具
├── shoplook2026-spi           # 模型与业务接口（单模块）
│   ├── model/*Entity.java     # 实体（BrandEntity、GoodEntity、OrderEntity ...）
│   ├── model/search/*SearchBean.java   # 查询条件（继承实体）
│   └── service/*Service.java  # 业务接口
├── shoplook2026-api           # 微服务实现（聚合）
│   ├── shoplook2026-brand-api / category-api / good-api / member-api
│   ├── shoplook2026-member-address-api / region-api / cart-api / order-api
│   ├── shoplook2026-user-api / rbac-api / upload-api / seckill-api
├── shoplook2026-gateway       # 网关（WebFlux，:8888）
```

各 api 服务内包结构：api（REST 控制器）、service/impl、mapper（MyBatis Mapper 接口 + XML）、config。

## 2.6 统一设计规范

### 2.6.1 实体与命名

- 实体：`XxxEntity`，位于 `com.situ.shoplook2026.spi.model`，继承 `AuditEntity`（审计字段 createdBy / createdTime / updatedBy / updatedTime，时间格式 yyyy-MM-dd HH:mm:ss）；
- 查询条件：`XxxSearchBean extends XxxEntity`，位于 `model/search`，模糊查询与范围条件在 XML 中以 `<if>` 动态拼接；
- 主键：统一 `Long`；
- 表字段下划线 ↔ 实体驼峰映射（MyBatis map-underscore-to-camel-case: on）。

### 2.6.2 统一响应

```
{
  "code": 200,        // 200 成功；非 200 失败（见 9 附录错误码）
  "success": true,    // 成功/失败标志
  "msg": "success",   // 消息
  "data": { ... }     // 业务数据（分页时为 PageInfo）
}
```

- 通用响应类：`JsonResp`（data 为 Object）、`TypedJsonResp<T>`（泛型，服务间 Feign 使用）；
- 分页：`PaginateInfo.from(pageNo, pageSize)`，pageSize 默认 0（page-size-zero: on 表示查询全部）；返回 `PageInfo`（list / total / pageNum / pageSize）。

### 2.6.3 接口前缀

- 网关路由前缀统一为 `/api/{资源}/**`（无版本前缀）；文件服务为 `/upload/**`；
- 控制器注解：`@RestController @RequestMapping("/api/xxx")`，`produces = application/json`。

### 2.6.4 统一异常与审计

- 全局异常处理：`@RestControllerAdvice` 统一包装为 `JsonResp`，避免堆栈外泄；
- 写操作统一填充 created_by / updated_by（取自登录上下文）与 created_time / updated_time。

---

# 3 数据库设计

## 3.1 设计说明

- 数据库：`shoplook2026`，字符集 utf8mb4，存储引擎 InnoDB；
- 数据对象共 22 个（不含 SKU/规格体系）；
- 主键 `id` 自增（BIGINT）；审计字段统一 `created_time / created_by / updated_time / updated_by`；
- 金额使用 DECIMAL(10,2)（标价、实价、秒杀价使用 DECIMAL(10,2)；订单金额 DECIMAL(10,2)）；
- 逻辑删除字段 `is_del`（商品、订单等）；布尔字段使用 `bit`/`tinyint(1)`；
- 设计扩展（参考 DDL 基础上，承接 SRS 已确认决策）：order 新增 `refund_status`（模拟退款记录，见 SRS 4.4 / 6.6）；seckill_good 扩展 `seckill_price / stock / sold / limit_per_user`（见 SRS 4.2.11）。

## 3.2 数据关系概述

- 分类树：category.parent_id 自关联；
- 商品：good.category_id → category，good.brand_id → brand；good_detail_pics.good_id → good；
- 购物车：cart.member_id → member，cart.good_id → good；
- 会员地址：member_address.member_account → member.account；
- 订单：order.member_account → member.account；order_item.order_id → order，order_item.good_id → good；
- 秒杀：seckill_good.seckill_id → seckill，seckill_good.good_id → good；order.seckill_no 关联秒杀流水；
- 地区：t_china_region.parent_id 自关联（省/市/区）；
- RBAC：user ↔ user_group ↔ group ↔ group_role ↔ role ↔ role_perm ↔ perm ↔ perm_resource ↔ resource；menu.resource_id → resource。

## 3.3 数据表设计

### 3.3.1 brand（商品品牌）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| name | VARCHAR(30) | 品牌名称（唯一） |
| company | VARCHAR(255) | 公司名称 |
| logo | VARCHAR(255) | Logo URL |
| site | VARCHAR(255) | 网址 |
| description | VARCHAR(1024) | 简介 |
| created_time / created_by / updated_time / updated_by | — | 审计 |

索引：name 唯一索引。

### 3.3.2 category（商品分类）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| parent_id | BIGINT | 父分类（0 为根） |
| name | VARCHAR(50) | 名称 |
| title | VARCHAR(100) | 标题 |
| tag | VARCHAR(100) | 标签 |
| icon | VARCHAR(255) | 图标 |
| summary | VARCHAR(500) | 摘要 |
| sort | INT | 排序 |
| description | VARCHAR(1024) | 简介 |
| 审计 | — | — |

索引：parent_id 普通索引。

### 3.3.3 good（商品 SPU）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| spu_no | VARCHAR(30) | 货号（唯一） |
| name | VARCHAR(100) | 名称 |
| alias | VARCHAR(100) | 别名 |
| summary | VARCHAR(500) | 摘要 |
| category_id | BIGINT | 分类 |
| brand_id | BIGINT | 品牌 |
| mark_price | DECIMAL(10,2) | 标价 |
| price | DECIMAL(10,2) | 实价 |
| qty | INT | 库存 |
| pic / pic2 | VARCHAR(255) | 主图 / 次图 |
| detail_pics | VARCHAR(2000) | 详情图 URL 集合 |
| detail | TEXT | 富文本详情 |
| is_take_down | TINYINT(1) | 是否下架 |
| is_hot | TINYINT(1) | 是否热销 |
| is_del | TINYINT(1) | 逻辑删除 |
| is_seckill | TINYINT(1) | 是否秒杀商品 |
| description | VARCHAR(1024) | 备注 |
| 审计 | — | — |

索引：category_id、brand_id、name 普通索引；spu_no 唯一索引；is_hot + is_del 组合（前台热销查询）。

### 3.3.4 good_detail_pics（商品详情图片）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| good_id | BIGINT | 商品 |
| url | VARCHAR(255) | 图片 URL |
| sort | INT | 排序 |
| description | VARCHAR(255) | 说明 |

索引：good_id 普通索引。

### 3.3.5 cart（购物车）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| member_id | BIGINT | 会员 |
| good_id | BIGINT | 商品 |
| qty | INT | 数量 |

索引：member_id + good_id 唯一索引（同一商品不重复加购，重复时累加数量）。

### 3.3.6 member（会员）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| account | VARCHAR(30) | 登录账号（唯一） |
| password | VARCHAR(100) | 密码（BCrypt） |
| enabled | TINYINT(1) | 是否启用 |
| name / pinyin / pinyin_untoned | VARCHAR | 姓名及拼音 |
| sex | VARCHAR(10) | 性别 |
| birthday | DATE | 生日 |
| height / weight / iq | TINYINT / DECIMAL | 身高/体重/IQ |
| qq / wechat / phone / email | VARCHAR | 联系方式 |
| native_place_id | BIGINT | 籍贯（地区） |
| card_id | VARCHAR(30) | 证件号 |
| wedlock / political_orientation | VARCHAR | 婚姻状况/政治面貌 |
| address_id / address_detail | BIGINT / VARCHAR | 现住地区/详细地址 |
| race / religion / nationality | VARCHAR | 民族/宗教/国籍 |
| portrait | VARCHAR(255) | 头像 |
| description | TEXT | 简介 |
| extra_info | JSON | 扩展信息 |
| last_login_time / last_login_ip | DATETIME / VARCHAR | 最近登录 |
| version | INT | 乐观锁版本 |
| 审计 | — | — |

索引：account 唯一索引；phone 普通索引。

### 3.3.7 member_address（会员收货地址）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| member_account | VARCHAR(30) | 会员账号 |
| receiver | VARCHAR(30) | 收货人 |
| phone | VARCHAR(20) | 手机号 |
| addr_id | BIGINT | 所在地区（省市区） |
| addr_detail | VARCHAR(255) | 详细地址 |
| is_default | TINYINT(1) | 是否默认 |

索引：member_account 普通索引。

### 3.3.8 order（订单）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_no | VARCHAR(32) | 订单号（雪花 ID，唯一） |
| seckill_no | VARCHAR(32) | 秒杀流水号（秒杀订单填写） |
| member_account | VARCHAR(30) | 下单会员账号 |
| total_pay | DECIMAL(10,2) | 应付总额 |
| pay_type | VARCHAR(20) | 支付方式（模拟支付） |
| alipay_trade_no | VARCHAR(64) | 支付交易号（模拟支付流水号） |
| checkout_time | DATETIME | 下单时间 |
| pay_time | DATETIME | 支付时间 |
| ship_time | DATETIME | 发货时间 |
| accept_time | DATETIME | 收货时间 |
| status | VARCHAR(20) | 订单状态（见 3.5.1） |
| receiver_addr_id | BIGINT | 收货地址（快照来源） |
| receiver_name / receiver_phone / receiver_addr_detail | VARCHAR | 收货人快照 |
| order_comment | VARCHAR(500) | 订单备注 |
| refund_status | VARCHAR(20) | 退款状态（无/退款中/已退款，模拟退款用） |
| is_del | TINYINT(1) | 逻辑删除 |
| description | VARCHAR(1024) | 备注 |
| 审计 | — | — |

索引：order_no 唯一索引；member_account + created_time 组合索引；status 普通索引。

### 3.3.9 order_item（订单明细）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单 |
| good_id | BIGINT | 商品 |
| deal_price | DECIMAL(10,2) | 成交价 |
| count | INT | 数量 |
| good_name / good_pic / good_desc | VARCHAR | 商品快照 |

索引：order_id 普通索引。

### 3.3.10 seckill（秒杀活动）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) | 活动名称 |
| enabled | TINYINT(1) | 是否启用 |
| start_time / end_time | DATETIME | 起止时间 |
| description | VARCHAR(1024) | 说明 |
| 审计 | — | — |

索引：start_time / end_time 普通索引。

### 3.3.11 seckill_good（秒杀活动商品）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| seckill_id | BIGINT | 秒杀活动 |
| good_id | BIGINT | 商品 |
| seckill_price | DECIMAL(10,2) | 秒杀价 |
| stock | INT | 秒杀库存 |
| sold | INT | 已售数量 |
| limit_per_user | INT | 每人限购数 |
| description | VARCHAR(1024) | 说明 |

索引：seckill_id 普通索引；seckill_id + good_id 唯一索引。

### 3.3.12 t_china_region（行政区划）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) | 名称 |
| parent_id | BIGINT | 父级 |
| sort_order | INT | 排序 |
| level | TINYINT | 层级（1 省/2 市/3 区） |
| description | VARCHAR(255) | 说明 |

索引：parent_id 普通索引。

### 3.3.13 user（后台用户）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| username | VARCHAR(30) | 用户名（唯一） |
| password | VARCHAR(100) | 密码（BCrypt） |
| avatar_url | VARCHAR(255) | 头像 |
| enabled | TINYINT(1) | 是否启用 |
| status | TINYINT | 账号状态 |
| locked | TINYINT(1) | 是否锁定 |
| user_expire_time / credential_expire_time | DATETIME | 账号/凭证过期时间 |
| login_times | INT | 登录次数 |
| last_login_time / last_login_ip | DATETIME / VARCHAR | 最近登录 |
| op_mode | INT | 操作模式 |
| description | VARCHAR(1024) | 说明 |
| 审计 | — | — |

索引：username 唯一索引。

### 3.3.14 RBAC 数据表

- t_rbac_group（用户组）：id、name（唯一）、enabled、description、审计；
- t_rbac_role（角色）：id、name（唯一）、enabled、description、审计；
- t_rbac_perm（权限）：id、name（唯一）、enabled、description、审计；
- t_rbac_resource（资源）：id、name、type（接口/按钮）、value（路径或按钮标识）、description、审计；type + value 唯一索引；
- t_rbac_menu（菜单）：id、parent_id、resource_id、name、icon、url、sort、description、审计；parent_id 索引；
- t_rbac_user_group（用户-组）：id、user_id、group_id；user_id + group_id 唯一索引；
- t_rbac_group_role（组-角色）：id、group_id、role_id；group_id + role_id 唯一索引；
- t_rbac_role_perm（角色-权限）：id、role_id、perm_id；role_id + perm_id 唯一索引；
- t_rbac_perm_resource（权限-资源）：id、resource_id、perm_id；resource_id + perm_id 唯一索引。

## 3.4 关键索引设计

- 商品列表检索：good（is_take_down, is_del, category_id / brand_id / price）组合索引，支撑前台筛选排序；
- 订单查询：order（member_account, created_time）组合索引，支撑"我的订单"按状态筛选；
- 秒杀抢购：seckill_good（seckill_id, good_id）唯一索引，防止重复关联；
- 登录查询：member.account、user.username 唯一索引。

## 3.5 数据字典

### 3.5.1 订单状态（order.status）

| 值 | 含义 | 说明 |
| --- | --- | --- |
| 待付款 | 已下单未支付 | 可支付、可取消；超时自动关闭 |
| 已支付 | 已支付待发货 | 后台发货、退款申请 |
| 待收货 | 已发货待收货 | 会员确认收货 |
| 已确认 | 已确认收货 | 终态 |
| 已取消 | 已取消 | 终态（含超时关闭） |

### 3.5.2 退款状态（order.refund_status）

无退款 / 退款中 / 已退款（模拟退款使用）。

### 3.5.3 商品状态

上架（is_take_down=0）/ 下架（is_take_down=1）；逻辑删除 is_del=1；热销 is_hot=1；秒杀标识 is_seckill=1。

### 3.5.4 秒杀活动状态

未开始 / 进行中 / 已结束 / 已禁用（由 enabled 与 start_time / end_time 推导）。

### 3.5.5 资源类型（t_rbac_resource.type）

接口（URL）/ 按钮（按钮标识）。

### 3.5.6 令牌类型

admin（后台用户）/ member（会员）。

---

# 4 接口设计

## 4.1 通用约定

- 协议：HTTPS（生产）/ HTTP（开发）；JSON；
- 认证：请求头 `Authorization: Bearer {token}`；
- 统一响应 JsonResp / TypedJsonResp（见 2.6.2）；错误码见附录 B；
- 分页：请求 pageNo（默认 1）、pageSize（默认 0=全部），响应 data 为 PageInfo；
- 公开接口白名单：登录/注册、商品与分类查询（GET）、地区查询、秒杀活动查询、文件读取；
- 写操作与会员数据接口须校验令牌及归属。

## 4.2 网关路由设计

| 路由前缀 | 目标服务 |
| --- | --- |
| `/api/brands/**` | shoplook2026-brand-api |
| `/api/categories/**` | shoplook2026-category-api |
| `/api/goods/**` | shoplook2026-good-api |
| `/api/members/**` | shoplook2026-member-api |
| `/api/member-addresses/**` | shoplook2026-member-address-api |
| `/api/regions/**` | shoplook2026-region-api |
| `/api/carts/**` | shoplook2026-cart-api |
| `/api/orders/**`, `/api/order-items/**` | shoplook2026-order-api |
| `/api/users/**` | shoplook2026-user-api |
| `/api/groups/**`, `/api/roles/**`, `/api/perms/**`,<br /> `/api/resources/**`, `/api/menus/**`, `/api/group-roles/**`,<br /> `/api/perm-resources/**`, `/api/role-perms/**`, `/api/user-groups/**` | shoplook2026-rbac-api |
| `/api/seckills/**` | shoplook2026-seckill-api |
| `/upload/**` | shoplook2026-upload-api |

## 4.3 认证与权限接口

| 接口功能 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 后台登录 | POST | /api/users/login | 公开 |
| 会员注册 | POST | /api/members/register | 公开 |
| 会员登录 | POST | /api/members/login | 公开 |
| 当前用户动态菜单 | GET | /api/menus/mine | 登录 |
| 权限判定（网关内部调用） | POST | /api/rbac/check | 服务间 |

说明：登录成功签发 JWT；网关 GlobalFilter 校验令牌并解析用户身份（userId、username、audience）放入请求头（X-User-Id、X-User-Name、X-Audience）；后台写接口由网关调用 rbac-api 权限判定接口按 path+method 匹配资源进行授权，未授权返回 403。

## 4.4 前台接口清单

| 接口功能 | 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- | --- |
| 商品分页 | GET | /api/goods | 关键字/分类/品牌/价格区间/排序/热销 | 公开 |
| 商品详情 | GET | /api/goods/id/{id}?full=true | 含品牌/分类/详情图 | 公开 |
| 分类树 | GET | /api/categories/tree | 前台导航 | 公开 |
| 首页热销 | GET | /api/goods?isHot=1 | 热销商品 | 公开 |
| 购物车列表 | GET | /api/carts | 当前会员 | 会员 |
| 加入购物车 | POST | /api/carts | {goodId, qty} | 会员 |
| 修改数量 | PUT | /api/carts | {id, qty} | 会员 |
| 删除购物车项 | DELETE | /api/carts | ids[] | 会员 |
| 地址列表 | GET | /api/member-addresses | 当前会员 | 会员 |
| 新增/编辑/删除地址 | POST/PUT/DELETE | /api/member-addresses | — | 会员 |
| 设置默认地址 | PUT | /api/member-addresses/{id}/default | — | 会员 |
| 提交订单 | POST | /api/orders | {cartIds[] 或 goodId+qty, addrId, comment} | 会员 |
| 我的订单 | GET | /api/orders?status= | 按状态分页 | 会员（本人） |
| 订单详情 | GET | /api/orders/id/{id} | 含明细 | 会员（本人） |
| 取消订单 | PUT | /api/orders/{id}/cancel | 待付款 | 会员（本人） |
| 确认收货 | PUT | /api/orders/{id}/confirm | 待收货 | 会员（本人） |
| 发起支付 | POST | /api/orders/{id}/pay | 返回模拟收银台信息 | 会员（本人） |
| 模拟支付确认 | POST | /api/orders/{id}/pay/confirm | 确认支付→已支付 | 会员（本人） |
| 秒杀活动列表 | GET | /api/seckills | 进行中/即将开始 | 公开 |
| 秒杀活动商品 | GET | /api/seckills/{id}/goods | 含秒杀价/剩余库存 | 公开 |
| 秒杀抢购 | POST | /api/seckills/grab | {seckillGoodId} | 会员 |
| 抢购结果查询 | GET | /api/seckills/grab/{orderNo} | 异步结果 | 会员 |
| 文件上传 | POST | /upload/** | multipart | 登录 |

## 4.5 后台接口清单

| 接口功能 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 品牌 CRUD | GET/POST/PUT/DELETE | /api/brands | RBAC |
| 分类 CRUD/树 | GET/POST/PUT/DELETE | /api/categories（…/tree） | RBAC |
| 商品 CRUD/上下架 | GET/POST/PUT/DELETE | /api/goods（…/take-down） | RBAC |
| 订单分页/详情 | GET | /api/orders | RBAC |
| 订单发货/取消/退款 | PUT | /api/orders/{id}/ship、/cancel、/refund | RBAC |
| 会员分页/详情/禁用/重置密码 | GET/PUT | /api/members | RBAC |
| 秒杀活动/商品 CRUD | GET/POST/PUT/DELETE | /api/seckills、/api/seckills/{id}/goods | RBAC |
| 后台用户 CRUD/分配组 | GET/POST/PUT/DELETE | /api/users、/api/user-groups | RBAC |
| 用户组/角色/权限/资源/菜单 CRUD | GET/POST/PUT/DELETE | /api/groups、/api/roles、/api/perms、<br />/api/resources、/api/menus、/api/group-roles、/api/role-perms、/api/perm-resources | RBAC |
| 地区树 | GET | /api/regions/tree | 登录 |
| 文件上传 | POST | /upload/** | 登录 |

## 4.6 关键接口详细定义

### 4.6.1 商品分页

```
GET /api/goods?pageNo=1&pageSize=12&keyword=手机&categoryId=5&brandId=2&priceLow=100&priceHigh=5000&sort=priceAsc
响应：
{
  "code": 200, "success": true, "msg": "success",
  "data": { "list": [ { "id":1, "name":"...", "price":2999.00, "markPrice":3299.00, "pic":"...", "isHot":true } ],
            "total": 36, "pageNum": 1, "pageSize": 12 }
}
```

### 4.6.2 提交订单

```
POST /api/orders
请求：{ "cartIds":[1,2], "addrId":5, "comment":"请尽快发货" }   // 或 { "goodId":3, "qty":1, ... }
响应：{ "code":200, "success":true, "msg":"success", "data":{ "orderNo":"1891...", "totalPay":2999.00 } }
```

### 4.6.3 模拟支付确认

```
POST /api/orders/{id}/pay/confirm
请求：{ "payType":"模拟支付" }
响应：{ "code":200, "success":true, "msg":"支付成功", "data":{ "status":"已支付", "payTradeNo":"MOCK2026..." } }
```

### 4.6.4 秒杀抢购

```
POST /api/seckills/grab
请求：{ "seckillGoodId":11 }
响应（提交成功，结果异步）：{ "code":200, "success":true, "msg":"抢购提交成功，请等待结果", "data":{ "orderNo":"1891...", "status":"处理中" } }
```

### 4.6.5 权限判定（服务间）

```
POST /api/rbac/check
请求：{ "userId":2, "path":"/api/orders/ship", "method":"PUT" }
响应：{ "code":200, "success":true, "data":{ "allowed":true } }
```

---

# 5 详细模块设计

## 5.1 模块职责总览

| 模块 | 主要职责 |
| --- | --- |
| gateway | 路由、JWT 校验、RBAC 判定、限流、CORS、统一异常 |
| brand-api | 品牌 CRUD、Logo 上传联动 |
| category-api | 分类树 CRUD、排序 |
| good-api | 商品 CRUD、分页检索、上下架、关联品牌/分类（Feign） |
| member-api | 会员注册/登录、资料维护、启用禁用、重置密码 |
| member-address-api | 收货地址 CRUD、默认地址 |
| region-api | 行政区划树查询 |
| cart-api | 购物车增删改查 |
| order-api | 下单、订单查询、状态机、模拟支付/退款、超时关单 |
| seckill-api | 秒杀活动/商品管理、Redis 预库存、Lua 扣减、异步下单 |
| user-api | 后台用户 CRUD、后台登录 |
| rbac-api | RBAC 关系 CRUD、权限判定、动态菜单 |
| upload-api | 文件上传、静态资源访问 |

## 5.2 公共模块设计（common / spi）

- common：`JsonResp`、`TypedJsonResp<T>`、`PaginateInfo`、`AuditEntity`、`Identity<T>`（实体主键接口）、通用常量与工具；
- spi：实体（XxxEntity）与查询条件（XxxSearchBean）、业务接口（XxxService），供各 api 服务依赖，避免重复定义与循环依赖。

## 5.3 商品中心

- brand-api：`BrandApi`（REST）→ `BrandService` → `BrandMapper`（XML）；列表支持名称/公司模糊；删除校验商品引用；
- category-api：`CategoryApi` 提供列表、树（/tree 递归组装）、CRUD；删除校验子分类与商品引用；
- good-api：`GoodApi` 提供分页检索（SearchBean 动态条件）、详情（full=true 时经 `BrandFeignService` / `CategoryFeignService` 组装品牌与分类）、CRUD、批量上下架、逻辑删除；
- 关联组装统一在 api 层通过 Feign 完成，spi 实体以非表字段（transient）存放关联对象。

## 5.4 会员中心

- member-api：注册（账号唯一校验、BCrypt 加密）、登录（校验密码、签发 member 令牌、更新登录信息）、资料查看/编辑（乐观锁 version）、修改密码、启用禁用、重置密码；
- member-address-api：按会员账号查询、CRUD、默认地址唯一性（同一会员仅一条 is_default=1）；
- region-api：树形查询（level 1/2/3）。

## 5.5 交易中心

### 5.5.1 cart-api

- 加入购物车：member_id + good_id 存在则累加数量，否则新增；校验商品上架与库存上限；
- 修改/删除：校验归属当前会员。

### 5.5.2 order-api

- 下单：事务内——校验商品（上架、未删、库存充足）→ 生成订单（雪花 ID）与订单明细（商品快照）→ 扣减库存（`UPDATE good SET qty=qty-# WHERE id=? AND qty>=#`）→ 清除购物车勾选项（异步）→ 返回订单号；
- 状态机：待付款 →（支付确认）→ 已支付 →（发货）→ 待收货 →（确认收货）→ 已确认；待付款可取消、超时自动关闭；状态流转做合法性校验；
- 模拟支付：发起支付返回模拟收银台信息；确认支付时校验订单与金额一致 → 写支付交易号（模拟流水号）与 pay_time → 置为已支付；
- 模拟退款：refund_status 无→退款中→已退款，记录处理状态；
- 超时关单：消费 RabbitMQ 延时消息（30 分钟）关闭待付款订单并回补库存，定时扫描兜底；
- 查询：按会员账号 + 状态分页；详情含订单明细。

## 5.6 秒杀中心（seckill-api）

- 活动/秒杀商品管理：CRUD，秒杀商品含秒杀价、秒杀库存、每人限购；
- 活动开始前：将秒杀商品库存预热到 Redis（key：`seckill:stock:{seckillGoodId}`，值=stock）；
- 抢购入口：校验活动时间窗与启用 → Redis + Lua 原子扣减（库存>0 → 扣减 → 记录用户已抢）→ 成功则发送 RabbitMQ 消息 → 异步创建秒杀订单（seckill_no 关联）→ 返回 orderNo 供前端轮询结果；
- 限购：Redis 记录 `seckill:user:{memberId}:{seckillGoodId}`，防止重复抢购；
- 超时未支付：与普通订单一致的 30 分钟关单，回补 seckill_good.stock 与 Redis 库存；
- 数据兜底：DB 侧以 `UPDATE seckill_good SET sold=sold+1, stock=stock-1 WHERE id=? AND stock>0` 条件更新防超卖。

## 5.7 权限中心（user-api / rbac-api）

- user-api：后台用户 CRUD、后台登录（校验用户名/密码/启用/锁定/过期，签发 admin 令牌）；
- rbac-api：
  - 关系维护：user_group / group_role / role_perm / perm_resource 及 menu 的 CRUD；
  - 权限判定：根据 path + method 匹配 t_rbac_resource.value → 资源关联的权限 → 角色 → 用户组 → 用户，判定是否放行；
  - 动态菜单：按用户权限返回菜单树。

## 5.8 系统支撑（gateway / upload-api）

- gateway（WebFlux）：GlobalFilter 校验 JWT（白名单放行）；解析身份注入请求头；后台写接口调用 rbac-api 权限判定；限流（令牌桶）；CORS（globalcors）；统一异常；
- upload-api：multipart 上传 → 校验类型/大小 → 存储至本地目录（配置项 upload.location）→ Redis 记录 → 返回可访问 URL；静态资源经网关 /upload/** 访问。

---

# 6 关键流程设计

## 6.1 认证鉴权流程

```
会员/后台用户 → 登录接口（member-api / user-api）校验凭证
  → 签发 JWT（audience=member/admin，有效期 7 天/12 小时）
  → 前端保存令牌并随请求携带 Authorization: Bearer
  → 网关 GlobalFilter 校验令牌（过期/非法 → 401）
  → 解析身份注入 X-User-Id / X-User-Name / X-Audience 请求头
  → 后台写接口：网关调用 rbac-api 权限判定（path+method → 资源 → 权限 → 角色 → 用户组 → 用户）
  → 未授权返回 403
```

## 6.2 下单与模拟支付流程

```
下单（购物车/立即购买）：
  校验商品 → 事务内创建订单+明细+扣库存 → 返回 orderNo
模拟支付：
  发起支付 → 返回模拟收银台
  会员确认支付 → 校验订单归属与金额 → 写支付交易号与支付时间 → 状态置为已支付
  （可选模拟异步通知：内部直接确认，保证幂等）
取消/超时：
  待付款可取消；超时 30 分钟经 RabbitMQ 延时消息 + 定时兜底关闭并回补库存
发货/收货：
  后台发货（已支付→待收货）→ 会员确认收货（待收货→已确认）
```

## 6.3 秒杀抢购流程

```
会员点击立即秒杀 → seckill-api 校验时间窗/启用/限购
  → Redis + Lua 原子扣减秒杀库存（防超卖）
  → 记录已抢（限购）→ 发送 RabbitMQ 异步下单消息
  → 返回 orderNo（前端轮询抢购结果）
  → 消费消息创建秒杀订单（seckill_no 关联）→ 进入支付流程
  → 超时 30 分钟未支付：关单并回补 Redis 与 DB 秒杀库存
```

## 6.4 数据一致性设计

- 事务边界：下单（订单 + 明细 + 扣库存）在同一本地事务；清购物车异步执行；
- 幂等：订单号唯一约束防重复下单；支付确认与 RabbitMQ 消费以订单号/流水号去重；
- 最终一致：秒杀异步下单、超时关单、退款均通过 RabbitMQ + 重试保证最终一致；
- 库存防超卖：DB 条件更新（`qty>=#` / `stock>0`）+ Redis Lua 原子扣减双重保障。

---

# 7 前端设计

## 7.1 工程结构

```
mall-web（PC 商城前端）        admin-web（后台管理系统）
├── src/                      ├── src/
│   ├── api/（接口封装）        │   ├── api/（接口封装）
│   ├── router/（路由）         │   ├── router/（路由，含动态菜单）
│   ├── stores/（Pinia）        │   ├── stores/（Pinia：用户/权限）
│   ├── views/（页面）          │   ├── views/（页面，按模块分目录）
│   ├── components/（组件）     │   ├── components/（Element Plus 封装）
│   └── utils/（Axios 等）      │   └── layout/（布局：侧边菜单/顶栏）
└── vite.config.js             └── vite.config.js
```

## 7.2 前端通用设计

- Axios 封装：统一 baseURL（反代到网关）、请求拦截器携带令牌、响应拦截器统一处理业务码与 401 跳登录、错误提示；
- 登录状态：Pinia 保存用户信息与令牌（持久化）；令牌过期自动跳转登录并回跳原页面；
- 后台动态菜单：登录后调用 /api/menus/mine 渲染侧边菜单，按按钮权限标识控制显隐/禁用。

## 7.3 页面-接口映射

| 页面 | 关键接口 |
| --- | --- |
| 首页 | 商品热销、分类树、秒杀活动 |
| 商品列表 | 商品分页 |
| 商品详情 | 商品详情、加购物车、立即购买 |
| 购物车 | 购物车 CRUD |
| 订单确认/支付 | 提交订单、地址列表、发起支付、支付确认 |
| 个人中心-订单 | 我的订单、取消、确认收货 |
| 后台各管理页 | 对应 /api 后台接口 |

---

# 8 非功能设计

## 8.1 性能设计

- 热点缓存：热销商品、分类树、秒杀活动与秒杀库存（Redis）；商品详情可短 TTL 缓存；
- 秒杀削峰：Redis + Lua 原子扣减、RabbitMQ 异步下单，控制 DB 压力；
- 分页：PageHelper 分页 + 索引优化，避免全表扫描；
- 目标：普通查询 P95 < 500ms；秒杀抢购提交 P95 < 100ms（指标承接 SRS 9.1）。

## 8.2 安全设计

- 密码 BCrypt；后台用户锁定与账号/凭证过期策略；
- JWT：HS256 签名（生产建议 RSA 公私钥），有效期 admin 12h / member 7d，支持刷新；
- RBAC：后台接口/菜单/按钮按资源授权；会员数据归属校验；
- 防刷：登录失败限流、秒杀抢购限流、验证码（P1）；
- 输入安全：SQL 参数化、XSS 过滤、文件上传类型/大小校验；
- 模拟支付安全：校验订单归属与金额一致性，防伪造支付结果。

## 8.3 可靠性与可用性

- 关键服务多实例部署，Nacos 健康检查剔除故障实例；OpenFeign 超时与容错；
- RabbitMQ：消费失败重试 + 死信队列，保证最终一致；
- 数据库主从/备份，每日备份；
- 监控：健康检查、日志链路（traceId）、生产接入 Prometheus + Grafana（P2）。

## 8.4 配置设计（Nacos）

- 分组 group=shoplook2026；namespace 按环境配置；
- 公共配置（数据源、MyBatis、PageHelper、RabbitMQ、Redis）由配置中心统一管理，各服务 application.yaml 仅保留服务名与端口。

---

# 9 附录

## 附录 A：错误码定义

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或令牌失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁/限流 |
| 500 | 系统内部错误 |
| 7001 | 库存不足 |
| 7002 | 未到秒杀时间或已结束 |
| 7003 | 已参与过该秒杀/超出限购 |
| 7004 | 秒杀已售罄 |
| 7005 | 订单状态不允许该操作 |

## 附录 B：页面清单映射

- PC 商城前端：注册、登录、首页、商品列表、商品详情、购物车、订单确认、支付（模拟收银台）、支付结果、个人中心（个人信息/收货地址/我的订单/订单详情/修改密码）、秒杀会场；
- 后台管理系统：登录、工作台、品牌、分类、商品、订单、会员、秒杀、系统管理（用户/角色/权限/资源/菜单/用户组）、地区、个人中心。

---

（完）
