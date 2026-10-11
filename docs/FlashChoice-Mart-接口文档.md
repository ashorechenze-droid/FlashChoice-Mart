# FlashChoice-Mart（极速优选商城）接口文档

> 适用版本：V1.3（四期联调：收藏商品降价/进入秒杀站内信通知，扩充 FCM-collect-microservice）
> 依据：《基于 Spring Cloud 的 B2C 微服务秒杀商城-详细设计说明书》V1.0.2
> 说明：本表所列接口均已按上述设计文档落地实现，可直接联调；标注「待实现」的能力暂未开放，详见文末【当前实现边界】。
> 前端工程：PC 商城前端（含管理端页面）位于 `FlashChoiceMart-frontend/mall-web`（Vue3 + Element Plus + Vite），开发代理 `/api`、`/upload` → 网关 8888。

---

## 1 通用约定

### 1.1 访问地址

| 环境 | 地址 | 说明 |
| --- | --- | --- |
| 网关（正式入口） | `http://<host>:8888` | 路由前缀 `/api/**`，如 `http://localhost:8888/api/goods` |
| 直连微服务（开发调试） | 见下表端口 | 绕过网关，需要手动携带身份请求头 |

| 服务 | 注册名 | 端口 | 路由前缀 |
| --- | --- | --- | --- |
| 品牌服务 | FCM-brand-microservice | 20001 | `/api/brands` |
| 分类服务 | FCM-category-microservice | 20002 | `/api/categories` |
| 商品服务 | FCM-good-microservice | 20003 | `/api/goods` |
| 会员服务 | FCM-member-microservice | 20004 | `/api/members` |
| 收货地址服务 | FCM-member-address-microservice | 20005 | `/api/member-addresses` |
| 行政区划服务 | FCM-region-microservice | 20006 | `/api/regions` |
| 购物车服务 | FCM-cart-microservice | 20007 | `/api/carts` |
| 订单服务 | FCM-order-microservice | 20008 | `/api/orders` |
| 后台用户服务 | FCM-user-microservice | 20009 | `/api/users` |
| 权限服务 | FCM-rbac-microservice | 20010 | `/api/groups` `/api/roles` `/api/perms` `/api/resources` `/api/menus` `/api/rbac` |
| 秒杀服务 | FCM-seckill-microservice | 20011 | `/api/seckills` |
| 文件上传服务 | FCM-upload-microservice | 20012 | `/upload` |
| 商品收藏/会员消息服务 | FCM-collect-microservice | 20013 | `/api/collects` `/api/notices` |

### 1.2 认证与身份传递

- 登录成功即签发**双 Token（JWT，HS256）**：`accessToken`（短效，请求携带）+ `refreshToken`（长效一次性，仅用于续签换新）。有效期：后台 `admin` 12 小时 / 7 天，会员 `member` 7 天 / 30 天。
- 受保护接口在请求头携带 `Authorization: Bearer {accessToken}`，由**网关统一校验**（签名/过期/签发者/令牌用途），任一不合法即返回 401 `{code:401, success:false, msg}`；**Refresh Token 仅用于刷新接口换新，携带它访问业务接口一律拒绝**（JWT Claims 中 `tokenType=access` 方可访问）。
- 校验通过后网关自动注入以下身份请求头再转发下游；**客户端伪造的同名头会被网关先清除**，下游只信任网关注入值：
  - `X-User-Id`：当前登录用户 ID（Long）
  - `X-User-Name`：当前登录用户账号/用户名（String）
  - `X-Audience`：令牌类型（`admin` 后台用户 / `member` 会员）
- **公开接口（免令牌）**：登录 / 注册 / 刷新令牌 / 获取图形验证码（见 §3.1、§6.1），以及品牌、分类、商品、地区、秒杀等 GET 查询与上传文件的读取（各接口表「权限」列标注「公开」）。
- 直连微服务（绕过网关开发调试）时无网关代注，需手动携带上述身份请求头；未带时接口按缺省处理（部分接口可用查询参数兜底，见各接口说明）。
- **网关防护链**（按执行顺序）：内部接口拦截（外部访问 403）→ 限流（认证入口 20 次/分/IP，其余 300 次/分/IP，超限 429）→ JWT 认证（401）→ RBAC 授权（仅 admin 令牌的写操作判定，未授权 403；判定结果本地缓存 60 秒，rbac-api 不可用时降级放行）。
- **内部接口**（仅供服务间 Feign 直连调用，网关拒绝外部访问并返回 403）：`PUT /api/goods/stock/deduct`、`PUT /api/goods/stock/restore`、`PUT /api/orders/close-timeout/**`、`POST /api/rbac/check`、`DELETE /api/rbac/users/*/groups`、`POST /api/notices/internal/**`。

### 1.3 统一响应结构

所有接口（除文件流）返回 JSON：

```json
{
  "code": 200,        // 200 成功；非 200 失败（见错误码表）
  "success": true,    // 成功/失败标志
  "msg": "success",   // 提示消息（失败时为错误原因）
  "data": { ... }     // 业务数据（分页时为 PageInfo，见下）
}
```

分页响应 `data` 为 PageHelper `PageInfo`，前端取以下核心字段即可：

```json
{
  "list": [ ... ],        // 当前页数据
  "total": 36,            // 总记录数
  "pageNum": 1,           // 当前页码
  "pageSize": 12,         // 每页条数（0 表示不分页查全部）
  "pages": 3              // 总页数
}
```

### 1.4 分页与通用参数

- 分页请求参数：`pageNo`（默认 1）、`pageSize`（默认 0 = 查询全部）。
- 时间格式：`yyyy-MM-dd HH:mm:ss`；金额：分页与实体返回均为 `DECIMAL`（元，两位小数）。

### 1.5 错误码

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或令牌失效 |
| 403 | 无权限（含操作他人数据） |
| 404 | 资源不存在 |
| 429 | 请求过于频繁/限流 |
| 500 | 系统内部错误 |
| 7001 | 库存不足 |
| 7002 | 未到秒杀时间或已结束 |
| 7003 | 已参与过该秒杀/超出限购 |
| 7004 | 秒杀已售罄 |
| 7005 | 订单状态不允许该操作 |
| 7006 | 图形验证码错误 |
| 7007 | 图形验证码已过期 |

> 业务校验失败以 400 抛出，统一包装为上述结构，`msg` 为可直接提示的用户文案。网关侧 401 / 403 / 429 的响应体结构与业务一致（HTTP 状态码与 code 相同）。

---

## 2 商品中心（公开查询 / 后台管理）

### 2.1 品牌管理（后台）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 品牌分页 | GET | `/api/brands` | 参数：`name`、`company`、`keyword`（名称/公司模糊）、`pageNo`、`pageSize` |
| 品牌详情 | GET | `/api/brands/id/{id}` | — |
| 新增品牌 | POST | `/api/brands` | Body：`{name(必填), company, logo, site, description}`；`logo` 可填 `POST /upload/image` 返回的 url |
| 修改品牌 | PUT | `/api/brands` | Body：`{id(必填), name, company, logo, site, description}`（空值不更新） |
| 删除品牌 | DELETE | `/api/brands` | Body：`[id1, id2, ...]`；存在商品引用时拒绝删除（经 good-api 按 brandId 计数校验） |

### 2.2 分类管理

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 全部分类 | GET | `/api/categories` | 平铺列表，后台表格用 |
| 分类树 | GET | `/api/categories/tree` | 递归树 `data: [{..., children:[...]}]`，前台导航/后台树选择；Redis 缓存 10 分钟，分类增删改后自动失效 |
| 子分类列表 | GET | `/api/categories/parent/{parentId}` | 按父级查询 |
| 分类详情 | GET | `/api/categories/id/{id}` | — |
| 新增分类 | POST | `/api/categories` | Body：`{parentId(默认0根), name, title, tag, icon, summary, sort, description}` |
| 修改分类 | PUT | `/api/categories` | Body：`{id, ...}`（空值不更新） |
| 删除分类 | DELETE | `/api/categories/{id}` | 存在子分类或商品引用（经 good-api 按 categoryId 计数校验）时拒绝 |

### 2.3 商品管理（前台检索公开）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 商品分页 | GET | `/api/goods` | 多条件检索，见下方参数说明 |
| 商品详情 | GET | `/api/goods/id/{id}?full=true` | `full=true` 时经 Feign 组装 `brand`/`category`/`detailPicList`（品牌/分类服务不可用时降级为空，不影响主体） |
| 批量查询 | GET | `/api/goods/ids?ids=1,2,3` | 服务间/购物车回显用 |
| 新增商品 | POST | `/api/goods` | Body 见商品字段表 |
| 修改商品 | PUT | `/api/goods` | Body：`{id, ...}`（空值不更新）；**实价下调**时自动向收藏该商品的会员下发降价站内信（通知失败不影响改价，见 §4.4） |
| 批量上/下架 | PUT | `/api/goods/take-down?ids=1,2&isTakeDown=true` | `isTakeDown=true` 下架 / `false` 上架 |
| 批量删除 | DELETE | `/api/goods` | Body：`[id1, id2]`（逻辑删除 `is_del=1`） |

**商品分页参数**（前台首页/列表页）：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| keyword | String | 匹配 name / alias / spuNo |
| categoryId | Long | 分类 |
| brandId | Long | 品牌 |
| priceLow / priceHigh | BigDecimal | 实价区间 |
| isHot | Boolean | 1 热销（首页热销：`?isHot=1&isTakeDown=0&isDel=0`） |
| isTakeDown | Boolean | 上下架过滤（前台应传 `0`） |
| isDel | Boolean | 逻辑删除过滤（前台应传 `0`） |
| isSeckill | Boolean | 秒杀商品过滤 |
| sort | String | `default` / `priceAsc` / `priceDesc` / `hot` |
| pageNo / pageSize | int | 分页 |

> ℹ️ 首页热销查询（`isHot=1&isTakeDown=0&isDel=0` 的纯分页请求）命中 Redis 短缓存（5 分钟），商品增删改/上下架后自动失效；其余检索直查数据库。

**商品主要字段**（Body 提交用）：`spuNo(货号), name, alias, summary, categoryId, brandId, markPrice(标价), price(实价), qty(库存), pic, pic2, detailPics(详情图URL集合), detail(富文本), isTakeDown, isHot, isDel, isSeckill, description`

> ℹ️ 图片字段（`pic` / `pic2` / `detailPics`）建议先调 `POST /upload/image` 上传获取 url（见 §7）；`name` / `alias` / `summary` 为纯文本字段，服务端做 XSS 标签剥离（`detail` 富文本保留 HTML）。

---

## 3 会员中心

### 3.1 会员（前台 + 后台）

| 接口 | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 获取图形验证码 | GET | `/api/members/captcha` | 公开 | 返回 `data: {captchaId, imageBase64}`（PNG Data URL）；验证码存 Redis，2 分钟有效 |
| 会员注册 | POST | `/api/members/register` | 公开 | Body：`{account(必填), password(必填), captchaId(必填), captcha(必填), name, phone, ...}`；先校验图形验证码，再做账号唯一校验 + BCrypt 加密 |
| 会员登录 | POST | `/api/members/login` | 公开 | Body：`{account, password, captchaId, captcha}`；先校验图形验证码（一次性），通过后签发双 Token，`data` 结构见下方 |
| 令牌刷新 | POST | `/api/members/refresh` | 公开 | Body：`{refreshToken}`；Refresh Token 一次性轮换（防重放），成功返回新的一对 Token |
| 退出登录 | POST | `/api/members/logout` | 登录 | 请求头携带 `Authorization: Bearer {accessToken}`；Body 可省 `{refreshToken}`；吊销当前 Access Token 并删除 Refresh Token 记录 |
| 我的资料 | GET | `/api/members/id/{id}` | 登录 | 会员 ID 查详情 |
| 修改资料 | PUT | `/api/members` | 登录 | Body：`{id, version, name, phone, portrait, ...}`；乐观锁 `version` 冲突返回失败 |
| 修改密码 | PUT | `/api/members/{id}/password` | 登录 | Body：`{oldPassword, newPassword}` |
| 会员分页 | GET | `/api/members` | 后台 | 参数：`account`/`name`/`phone`/`keyword` + 分页 |
| 启用/禁用 | PUT | `/api/members/{id}/enabled?enabled=false` | 后台 | — |
| 重置密码 | PUT | `/api/members/{id}/reset-password?newPassword=xxx` | 后台 | BCrypt 加密后重置 |

**登录成功响应 `data` 结构**（会员登录；后台登录首字段为 `user`，其余相同）：

```jsonc
{
  "member": { "id": 1, "account": "zhangsan", "name": "张三", "enabled": true },  // 会员完整信息（密码不返回）
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   // Access Token：7 天有效，请求头 Authorization: Bearer 携带
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",  // Refresh Token：30 天有效，一次性使用，仅用于刷新换新
  "tokenType": "Bearer",
  "expiresIn": 604800                          // Access Token 剩余有效秒数
}
```

> 图形验证码规则：`captchaId` 与 `captcha` 须与 `GET /api/members/captcha` 返回的配对使用；验证码 2 分钟内有效，**校验通过后立即删除（一次性）**，重复提交或已过期返回 7007，文本不匹配返回 7006（错误码见 §1.5）。

> 安全联动：修改密码 / 后台重置密码 / 后台禁用会员时，该账号全部已签发令牌立即吊销（需重新登录）。吊销依赖 Redis 黑名单，Redis 不可用时服务降级为仅删除 Refresh Token 记录，Access Token 由自身有效期兜底（见 §9）。

### 3.2 收货地址（会员本人）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 地址列表 | GET | `/api/member-addresses` | 当前会员；身份头 `X-User-Name`，无头时可传 `?account=xxx` |
| 地址详情 | GET | `/api/member-addresses/id/{id}` | — |
| 新增地址 | POST | `/api/member-addresses` | Body：`{memberAccount(可省，取身份头), receiver, phone, addrId(地区ID), addrDetail, isDefault}` |
| 修改地址 | PUT | `/api/member-addresses` | Body：`{id, ...}` |
| 删除地址 | DELETE | `/api/member-addresses/{id}` | — |
| 设为默认 | PUT | `/api/member-addresses/{id}/default` | 同一会员仅一条默认（先清后置），需身份头/`account` |

### 3.3 行政区划（公开）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 地区树 | GET | `/api/regions/tree` | 省/市/区三级树 |
| 子级列表 | GET | `/api/regions?parentId=0` | 级联下拉：`parentId` 缺省 0 查省级 |

---

## 4 交易中心

### 4.1 购物车（会员本人）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 购物车列表 | GET | `/api/carts` | 当前会员；身份头 `X-User-Id`，无头时 `?memberId=`；`data[].good` 经 good-api Feign 回显 |
| 购物车条目 | GET | `/api/carts/id/{id}` | — |
| 加入购物车 | POST | `/api/carts` | Body：`{goodId, qty, memberId(可省)}`；同商品已存在则累加数量；经 Feign 校验商品在售与库存上限 |
| 修改数量 | PUT | `/api/carts` | Body：`{id, qty}`；校验归属，操作他人数据返回 403 |
| 删除条目 | DELETE | `/api/carts` | Body：`[id1, id2]`；校验归属 |

> ℹ️ `data[].good`（商品信息回显）经 good-api Feign（`GET /api/goods/ids` 批量查询）组装；商品服务不可用时降级为 `null`，不影响列表主体。

### 4.2 订单（下单/支付/状态流转）

| 接口 | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 我的订单/订单分页 | GET | `/api/orders` | 会员/后台 | 会员：身份头 `X-User-Name`（或 `memberAccount`）自动过滤本人；后台不带头为全量。参数：`status`、`orderNo`、`keyword`、`beginTime`、`endTime`、`pageNo`、`pageSize` |
| 订单详情 | GET | `/api/orders/id/{id}?withItems=true` | 本人/后台 | 含 `orderItems` 明细 |
| 按订单号查 | GET | `/api/orders/no/{orderNo}?withItems=true` | 本人/后台 | 秒杀结果轮询等场景 |
| 提交订单 | POST | `/api/orders` | 会员 | Body 见下方；返回 `data` 为订单实体 |
| 发起支付 | PUT | `/api/orders/{id}/pay` | 会员本人 | 返回模拟收银台信息 |
| 模拟支付确认 | POST | `/api/orders/{id}/pay/confirm` | 会员本人 | Body：`{payType:"模拟支付"}`；幂等 |
| 取消订单 | PUT | `/api/orders/{id}/cancel` | 会员本人/后台 | 仅「待付款」；会员校验订单归属，后台令牌跳过；取消后经 good-api Feign 按明细回补库存 |
| 发货 | PUT | `/api/orders/{id}/ship` | 仅后台 | 「已支付」→「待收货」；会员令牌调用返回 403 |
| 确认收货 | PUT | `/api/orders/{id}/confirm` | 会员本人 | 「待收货」→「已确认」 |
| 申请退款 | PUT | `/api/orders/{id}/refund` | 会员本人/后台 | Body：`{reason}`；「无退款」→「退款中」（模拟）；会员校验订单归属 |
| 确认退款 | PUT | `/api/orders/{id}/refund/confirm` | 仅后台 | 「退款中」→「已退款」；会员令牌调用返回 403 |
| 超时关单 | PUT | `/api/orders/close-timeout/{orderNo}` | 内部 | RabbitMQ 延时消息消费入口（网关拒绝外部访问）；关单后经 good-api Feign 回补库存 |
| 批量删除 | DELETE | `/api/orders` | 仅后台 | Body：`[id1]`；逻辑删除；会员令牌调用返回 403 |

**提交订单 Body 两种方式**：

```jsonc
// 方式一：购物车结算
{ "cartIds": [1, 2], "addrId": 5, "comment": "请尽快发货" }
// 方式二：立即购买
{ "goodId": 3, "qty": 1, "addrId": 5, "comment": "请尽快发货" }
// memberAccount/memberId 可不传：网关注入 X-User-Name / X-User-Id 时自动取身份
```

> ℹ️ **下单内部链路（Feign）**：`member-address-api` 取收货地址快照（缺失/非本人拒绝）→ `cart-api` 按 `cartIds` 取勾选条目（购物车结算，校验归属）→ `good-api` 校验在售并快照名称/价格/图片 → `good-api` **条件扣减库存**（`qty >= count` 防超卖，任一不足整体失败）→ 订单与明细落库（落库失败补偿回补库存）→ `cart-api` 清除勾选条目（失败降级，不影响下单结果）。购物车结算依赖网关注入的 `X-User-Id` 做归属校验与清除。

> ℹ️ **超时关单**：下单成功后发送 RabbitMQ 延时消息（TTL + 死信队列实现 30 分钟延时投递），消费端调用 `PUT /api/orders/close-timeout/{orderNo}` 幂等关单；另有每分钟定时扫描兜底，二者互补。订单号 `orderNo` 由雪花 ID 生成（时间有序、全局唯一）。

**模拟支付确认**：

```jsonc
POST /api/orders/{id}/pay/confirm
Body: { "payType": "模拟支付" }
// 响应 data 为 null，msg: 支付成功
```

**订单状态字典**：`待付款 → 已支付 → 待收货 → 已确认`（终态）；`已取消`（终态，含超时关闭）。退款状态：`无退款 / 退款中 / 已退款`。

### 4.3 商品收藏（会员本人）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 收藏列表 | GET | `/api/collects` | 当前会员，按收藏时间倒序（id 倒序）；身份头 `X-User-Id`，无头时 `?memberId=`；`data[].good` 经 good-api Feign 回显 |
| 收藏状态查询 | GET | `/api/collects/check?goodId={goodId}` | 返回 `data: true/false`（商品详情页/卡片星标状态用）；未登录或参数缺失返回 false |
| 收藏商品 | POST | `/api/collects` | Body：`{goodId, memberId(可省，取身份头)}`；**幂等**：已收藏直接成功（库唯一索引 `member_id + good_id`）；经 Feign 校验商品（不存在 404 / 已下架 400；商品服务不可用时降级放行） |
| 取消收藏（按商品） | DELETE | `/api/collects/good/{goodId}` | 商品详情页/收藏列表取消用；幂等（未收藏时删除 0 行） |
| 批量取消收藏 | DELETE | `/api/collects` | Body：`[id1, id2]`（收藏条目 id）；仅删除当前会员名下条目（SQL 按 `member_id` 过滤） |

> ℹ️ 收藏关系由唯一索引 `uk_collect_member_good(member_id, good_id)` 防重；`data[].good`（商品信息回显）经 good-api Feign（`GET /api/goods/ids` 批量查询）组装，商品服务不可用时降级为 `null`，不影响列表主体（同购物车）。

### 4.4 消息通知（会员本人，站内信）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 通知列表 | GET | `/api/notices` | 当前会员，按 id 倒序；`?unreadOnly=true` 仅未读；身份头 `X-User-Id`，无头时 `?memberId=` |
| 未读数量 | GET | `/api/notices/unread-count` | 返回 `data: 3`（消息中心红点用）；未登录返回 `0` |
| 标记已读 | PUT | `/api/notices/{id}/read` | 单条标记（SQL 按 `member_id` 过滤，仅能操作本人通知）；返回更新行数 |
| 全部已读 | PUT | `/api/notices/read-all` | 当前会员名下未读全部置已读；返回更新行数 |

**通知数据结构**（`data` 为列表；审计字段略）：

```jsonc
{
  "id": 2,
  "memberId": 1,
  "type": 1,               // 1 降价 / 2 进入秒杀
  "goodId": 2,
  "title": "收藏商品降价提醒",
  "content": "你收藏的「智能手表 S8 运动版」降价了：￥1999 → ￥1499，快来看看吧",
  "isRead": false,
  "createdTime": "2026-09-14 09:12:00"
}
```

**触发链路（内部接口，外部经网关访问 403）**：

| 触发场景 | 调用方 | 内部接口 | 参数 |
| --- | --- | --- | --- |
| 后台改价**实价下调**（good-api 改价时对比旧记录检测） | FCM-good-microservice | POST `/api/notices/internal/price-drop` | `{goodId, goodName(可空), oldPrice, newPrice}` |
| 商品加入秒杀活动（seckill-api 保存成功后） | FCM-seckill-microservice | POST `/api/notices/internal/seckill-join` | `{goodId, goodName(可空), seckillName, seckillPrice}` |

> ℹ️ 下发流程：调用方 Feign → 收藏服务经 `CollectService.listMemberIdsByGoodId` 查出收藏该商品的全部会员 → 逐人写入 notice 表（同批统一 `createdTime`）→ 返回下发条数。降价仅在下调且商品未下架/未删除时触发；`goodName` 为空时文案回退「商品」、秒杀活动名缺失回退「限时秒杀」；通知失败仅记日志告警，不影响改价与保存主流程。前端「消息中心」位于会员中心（收藏页签旁）：菜单角标显示未读数，列表按类型展示降价/秒杀标签，支持单条点击已读、「全部已读」与跳转商品详情。

---

## 5 秒杀中心（活动/秒杀商品 CRUD + 前台查询）

### 5.1 秒杀活动（后台管理）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 活动分页 | GET | `/api/seckills` | 参数：`keyword`（名称模糊）、`enabled`（true/false）、`pageNo`、`pageSize`；按 id 倒序 |
| 活动详情 | GET | `/api/seckills/id/{id}?withGoods=true` | `withGoods=true`（默认）携带 `seckillGoods` 列表 |
| 新增活动 | POST | `/api/seckills` | Body：`{name(必填), enabled(默认true), startTime(必填), endTime(必填), description}`，时间格式 `yyyy-MM-dd HH:mm:ss`，开始须早于结束 |
| 修改活动 | PUT | `/api/seckills` | Body：`{id(必填), name, enabled, startTime, endTime, description}`（空值不更新） |
| 启用/禁用 | PUT | `/api/seckills/{id}/enabled?enabled=true` | 禁用后前台不可见 |
| 删除活动 | DELETE | `/api/seckills/{id}` | 级联删除活动商品 |

### 5.2 秒杀活动商品（后台管理 / 前台查询）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 活动商品列表 | GET | `/api/seckills/{seckillId}/goods` | 公开。`data[].sold/stock` 计算剩余库存；`data[].good` 经 good-api Feign 回显（服务不可用时为空） |
| 秒杀商品详情 | GET | `/api/seckills/goods/{id}` | 公开。秒杀详情页/下单使用 |
| 新增活动商品 | POST | `/api/seckills/{seckillId}/goods` | Body：`{goodId(必填), seckillPrice(必填≥0), stock(必填≥0), sold(默认0), limitPerUser(默认1), description}`；同一活动重复加同一商品返回 400；保存成功后自动向收藏该商品的会员下发进入秒杀站内信（见 §4.4） |
| 修改秒杀商品 | PUT | `/api/seckills/goods` | Body：`{id(必填), seckillPrice, stock, sold, limitPerUser, description}`；**关联关系（活动/商品）不可变更**，需调整请删除后重建 |
| 删除秒杀商品 | DELETE | `/api/seckills/goods/{id}` | — |

**请求示例（新增活动）**：

```jsonc
POST /api/seckills
Body: {
  "name": "618 年中大促",
  "startTime": "2026-06-18 00:00:00",
  "endTime": "2026-06-18 23:59:59",
  "description": "全场秒杀"
}
// 响应 data: { "id": 1, ... }
```

**请求示例（新增活动商品）**：

```jsonc
POST /api/seckills/1/goods
Body: { "goodId": 10, "seckillPrice": 1999.00, "stock": 50, "limitPerUser": 1 }
// 响应 data: { "id": 21, "seckillId": 1, "goodId": 10, "seckillPrice": 1999.00, "stock": 50, "sold": 0, "limitPerUser": 1 }
```

**前台会场联调路径**：`GET /api/seckills/active`（进行中/即将开始）→ `GET /api/seckills/{id}/goods`（秒杀价和剩余库存）→ `POST /api/seckills/grab`（会员，Body：`{seckillGoodId, addrId}`；返回 `data: {orderNo, status:"处理中"}`，其中 `orderNo` 为秒杀流水号）→ `GET /api/seckills/grab/{orderNo}`（会员本人查询，返回 `data: {orderNo, status:"处理中|成功|失败", actualOrderNo}`）。抢购成功后从「我的订单」支付。管理员可调用 `POST /api/seckills/{id}/warm-up` 预热库存；首次抢购也会自动预热。

---

## 6 权限中心（后台管理）

### 6.1 后台用户

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 获取图形验证码 | GET | `/api/users/captcha` | 公开。返回 `data: {captchaId, imageBase64}`；机制同 §3.1（会员/后台各自独立生成，互不串用） |
| 后台登录 | POST | `/api/users/login` | 公开。Body：`{username, password, captchaId, captcha}`；先校验图形验证码（一次性），再校验启用/锁定/过期，签发双 Token，`data` 结构同 §3.1（首字段为 `user`） |
| 令牌刷新 | POST | `/api/users/refresh` | 公开。Body：`{refreshToken}`；一次性轮换，成功返回新的一对 Token |
| 退出登录 | POST | `/api/users/logout` | 登录。请求头 `Authorization: Bearer {accessToken}`；Body 可省 `{refreshToken}` |
| 用户分页 | GET | `/api/users` | 参数：`username`/`keyword` + 分页 |
| 用户详情 | GET | `/api/users/id/{id}` | — |
| 新增用户 | POST | `/api/users` | Body：`{username(必填), password(必填), avatarUrl, enabled, status, locked, description}` |
| 修改用户 | PUT | `/api/users` | Body：`{id, ...}`（空值不更新；密码字段不参与更新） |
| 启用/禁用 | PUT | `/api/users/{id}/enabled?enabled=false` | — |
| 重置密码 | PUT | `/api/users/{id}/reset-password?newPassword=xxx` | BCrypt 加密 |
| 批量删除 | DELETE | `/api/users` | Body：`[id1]`；删除前级联清理用户-组关联（调 rbac-api） |

### 6.2 RBAC 基础数据（用户组/角色/权限/资源）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 列表 | GET | `/api/groups`、`/api/roles`、`/api/perms`、`/api/resources?type=接口\|按钮` | resources 支持按 type 过滤 |
| 详情 | GET | `/api/groups/{id}`、`/api/roles/{id}`、`/api/perms/{id}`、`/api/resources/{id}` | — |
| 新增 | POST | 同上集合路径 | Body：`{name, enabled, description}`（resource 为 `{name, type, value, description}`，type+value 唯一） |
| 修改 | PUT | 同上 | Body：`{id, ...}` |
| 删除 | DELETE | `/{id}` | 存在下游引用时拒绝 |

### 6.3 菜单与动态菜单

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 全部菜单树 | GET | `/api/menus/tree` | 后台菜单维护 |
| 我的菜单 | GET | `/api/menus/mine` | 当前登录用户动态菜单（按授权资源过滤；身份头 `X-User-Id`，无头时可 `?userId=`） |
| 菜单详情/新增/修改/删除 | GET/POST/PUT/DELETE | `/api/menus`、`/api/menus/{id}` | Body：`{parentId(默认0), resourceId, name, icon, url, sort, description}`；删除存在子菜单时拒绝 |

### 6.4 授权分配（覆盖式）

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 用户分配组 | PUT | `/api/rbac/users/{userId}/groups` | Body：`[groupId, ...]` 整组覆盖 |
| 查询用户组 | GET | `/api/rbac/users/{userId}/groups` | 返回 `[groupId, ...]` |
| 组分配角色 | PUT | `/api/rbac/groups/{groupId}/roles` | Body：`[roleId, ...]` |
| 查询组角色 | GET | `/api/rbac/groups/{groupId}/roles` | — |
| 角色分配权限 | PUT | `/api/rbac/roles/{roleId}/perms` | Body：`[permId, ...]` |
| 查询角色权限 | GET | `/api/rbac/roles/{roleId}/perms` | — |
| 权限分配资源 | PUT | `/api/rbac/perms/{permId}/resources` | Body：`[resourceId, ...]` |
| 查询权限资源 | GET | `/api/rbac/perms/{permId}/resources` | — |
| 我的资源标识 | GET | `/api/rbac/mine/resources` | 当前登录用户的资源标识列表（按钮标识如 `good:add` 与接口路径），后台前端按此控制按钮显隐；身份头 `X-User-Id`，无头时可 `?userId=` |
| 清空用户组关联 | DELETE | `/api/rbac/users/{userId}/groups` | 内部接口（网关拒绝外部访问）。user-api 删除用户前级联清理 |
| 权限判定 | POST | `/api/rbac/check` | 网关内部调用（外部访问 403）。Body：`{userId, path, method}` → `data:{allowed: true/false}` |

---

## 7 文件上传（upload 服务）

| 接口 | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 图片上传 | POST | `/upload/image` | 登录 | multipart 字段 `file`；校验扩展名白名单与大小上限后存储；返回 `data: {url, name, size}` |
| 文件读取 | GET | `/upload/files/{filename}` | 公开 | 按文件流返回（自动 Content-Type）；文件名白名单校验，杜绝路径穿越 |

**上传约束**：扩展名白名单 `jpg/jpeg/png/gif/webp/bmp`；单文件 ≤ 5MB；存储文件名为服务端生成的 UUID（防覆盖）；存储目录由配置项 `upload.location` 指定（默认用户主目录 `fcm-upload`）。

**返回示例**：

```jsonc
POST /upload/image  (multipart/form-data: file=@goods.png)
// 响应 data
{ "url": "/upload/files/3f2b8c9e....png", "name": "goods.png", "size": 20480 }
```

> 使用方式：把 `url`（相对路径，经网关访问）填入商品 `pic` / `pic2` / `detailPics`、品牌 `logo` 等图片字段；前台渲染直接用 `<img :src="url">`（经网关 `/upload/**` 路由读取）。

---

## 8 典型联调流程（前台商城）

1. **注册/登录**：先调 `GET /api/members/captcha` 取图形验证码（展示 `imageBase64` 供用户输入，保留 `captchaId`）→ `POST /api/members/register`（Body 携带 `captchaId` + 用户输入的 `captcha`）→ `POST /api/members/login`（同样携带验证码两字段），保存返回的 `accessToken` / `refreshToken`；后续调用「登录」权限接口时请求头携带 `Authorization: Bearer {accessToken}`；若返回 401（Access Token 过期），先调 `POST /api/members/refresh` 换新再重试；退出调 `POST /api/members/logout`。后台端同理：先 `GET /api/users/captcha` 再 `POST /api/users/login` → 携带双 Token（admin Access Token 12 小时有效）。
2. **首页**：`GET /api/categories/tree`；`GET /api/goods?isHot=1&isTakeDown=0&isDel=0`；`GET /api/seckills/active`。
3. **商品列表/详情**：`GET /api/goods?...`；`GET /api/goods/id/{id}?full=true`。
4. **购物车**：`POST /api/carts {goodId, qty}` → `GET /api/carts` → `PUT /api/carts {id, qty}` / `DELETE /api/carts [ids]`。
5. **商品收藏**：`POST /api/collects {goodId}` 收藏（重复收藏幂等）→ `GET /api/collects` 我的收藏列表 → `GET /api/collects/check?goodId={goodId}` 查询收藏状态 → `DELETE /api/collects/good/{goodId}` 取消收藏。
6. **消息通知（站内信）**：后台对收藏商品改价（实价下调）或将其加入秒杀活动后，系统自动向收藏该商品的会员下发站内信；会员端 `GET /api/notices?unreadOnly=true` 拉取列表 → `GET /api/notices/unread-count` 未读数红点 → `PUT /api/notices/{id}/read` / `PUT /api/notices/read-all` 标记已读（触发链路见 §4.4）。
7. **收货地址**：`GET /api/member-addresses`；`POST /api/member-addresses`；`PUT /api/member-addresses/{id}/default`。
8. **提交订单**：`POST /api/orders {cartIds, addrId, comment}`（内部完成地址/商品快照、库存扣减与购物车清除）→ 取返回 `data.orderNo`。
9. **支付**：`PUT /api/orders/{id}/pay` 取收银台信息 → `POST /api/orders/{id}/pay/confirm {payType:"模拟支付"}`。
10. **我的订单**：`GET /api/orders?status=待付款`；取消 `PUT /api/orders/{id}/cancel`；确认收货 `PUT /api/orders/{id}/confirm`。
11. **前端工程（mall-web）**：位于 `FlashChoiceMart-frontend/mall-web`，`npm install && npm run dev` 后访问 `http://localhost:5173`；开发代理 `/api`、`/upload` → `http://localhost:8888`（无需处理跨域）；登录态存于 localStorage，Access Token 过期时自动用 Refresh Token 刷新并重放请求。「我的收藏」页面（会员中心）：登录后自动拉取收藏列表，支持商品详情页/列表卡片星标收藏、取消收藏与加入购物车；「消息中心」页签：展示降价/秒杀站内信（类型标签、未读红点、菜单角标），支持「全部已读」与点击条目跳转商品详情。

---

## 9 当前实现边界（重要）

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 各微服务 CRUD/查询接口 | ✅ 已实现 | 本文档全部列出 |
| 秒杀活动/秒杀商品 | ✅ 已实现 CRUD | 活动启用/禁用、级联删除、前台活动列表与活动商品查询 |
| 会员/后台用户登录（双 Token） | ✅ 已实现 | 登录签发 Access+Refresh；refresh 一次性轮换；logout 登出；改密/重置/禁用联动强制下线 |
| 图形验证码（登录/注册防刷） | ✅ 已实现 | EasyCaptcha 生成（120×40、5 字符）；Redis 键 `fcm:auth:captcha:{captchaId}`，TTL 2 分钟；校验通过即删除防复用；错误 7006 / 过期 7007；会员与后台门户各提供 `GET /captcha`（网关白名单放行，见 §3.1、§6.1） |
| 网关 JWT 认证 | ✅ 已实现 | 白名单/公开 GET 放行、验签（含令牌用途 `access/refresh` 区分）、Redis 黑名单校验、注入/防伪身份头（见 §1.2） |
| 网关 RBAC 授权过滤 | ✅ 已实现 | 仅 admin 令牌写操作调用 `/api/rbac/check`（WebClient 非阻塞）；判定结果本地缓存 60 秒；未授权 403；rbac-api 不可用时降级放行 |
| 网关限流与内部接口保护 | ✅ 已实现 | Redis 计数窗口：认证入口 20 次/分/IP、其余 300 次/分/IP，超限 429；库存扣减/回补、超时关单等内部接口网关层拒绝外部访问（403） |
| 文件上传 | ✅ 已实现 | upload 服务（20012）：类型/大小白名单、UUID 文件名防覆盖、路径穿越防护；读取公开、上传需登录（见 §7） |
| 令牌吊销（Redis 黑名单） | 🟡 依赖 Redis | 登出/改密/重置/禁用即时吊销；Redis 不可用时网关降级放行、服务降级删记录，由令牌自身有效期兜底 |
| 秒杀抢购链路 | ✅ 已实现 | 提交、结果轮询、Redis 预热、RabbitMQ 异步下单及取消/超时库存回补；真实依赖联调与压测仍需在目标环境完成 |
| 商品收藏 | ✅ 已实现 | 收藏 / 取消 / 列表 / 状态查询（§4.3）；唯一索引防重复、重复收藏幂等；商品信息经 good-api Feign 回显（服务不可用时降级 `null`） |
| 收藏商品消息推送（站内信） | ✅ 已实现 | 改价实价下调（good-api 对比旧记录检测）/ 加入秒杀活动（seckill-api 保存成功后）→ Feign 内部接口 → 收藏服务向收藏者批量写 notice（§4.4）；通知失败仅告警、不影响主流程；内部接口经网关 403 收口 |
| 购物车/秒杀商品列表的商品信息回显 | ✅ 已接通 | 经 good-api 跨服务 Feign 批量组装；商品服务不可用时降级为空 `good`（列表主体不受影响） |
| 下单全链路（地址/明细快照、库存扣减、购物车清除） | ✅ 已接通 | 经 member-address/good/cart 三个服务 Feign 实现；库存为条件扣减（`qty >= count`）防超卖，取消/超时关单自动回补；购物车清除失败降级 |
| 商品详情 full 组装（品牌/分类） | ✅ 已接通 | 详情图已返回；品牌/分类经 good-api Feign（brand/category 服务）组装 |
| 订单超时自动关单 | ✅ 已实现 | RabbitMQ 延时消息（TTL + 死信队列，30 分钟）为主链路，消费端幂等关单；每分钟定时扫描兜底 |
| 热点数据缓存 | ✅ 已实现 | 首页热销（5 分钟）、分类树（10 分钟）、秒杀进行中活动（1 分钟）；写操作后自动失效；Redis 不可用时降级直查 |
| Feign 超时与容错 | ✅ 已实现 | 连接 3s / 读取 5s 快速失败；业务层降级（回显为空等）与提示 |
| 输入安全（XSS 过滤） | ✅ 已实现 | 纯文本字段（商品名称/别名/摘要、订单留言、退款原因）服务端剥离 HTML 标签；`detail` 富文本保留 |
| 订单号生成 | ✅ 雪花 ID | `orderNo` 由 `SnowflakeIdGenerator` 生成（时间有序、全局唯一） |

---

（完）
