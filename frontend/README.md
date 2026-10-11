# 极速优选 · PC 商城前端（mall-web）

基于《基于 Spring Cloud 的 B2C 微服务秒杀商城 详细设计说明书》实现的 PC 商城会员端前端工程。

技术栈：Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios

## 快速开始

```bash
npm install        # 安装依赖（已安装可跳过）
npm run dev        # 开发模式 http://localhost:5173
npm run build      # 生产构建（产物在 dist/）
npm run preview    # 预览构建产物
```

## 后端联调开关

接口层封装了统一的 mock 降级：后端未启动时页面也能完整演示。

| 文件 | 作用 |
| --- | --- |
| `src/config.js` | `USE_MOCK = true` 使用本地 mock 数据；`false` 走真实网关 |
| `vite.config.js` | 开发代理：`/api`、`/upload` 反代到网关 `http://localhost:8888` |

联调步骤：
1. 启动后端微服务（网关 :8888，路由前缀 `/api/**`，见设计文档 2.4 / 4.2）；
2. 将 `src/config.js` 的 `USE_MOCK` 改为 `false`；
3. `npm run dev` 即可对接真实接口（Axios 已封装 JWT 携带与 401 处理）。

## 页面清单

| 路由 | 页面 | 关键接口（设计文档 4.4） |
| --- | --- | --- |
| `/` | 首页（打字机 / 轮播 / 分类 / 热销 / 秒杀条） | GET /api/goods?isHot=1、GET /api/categories/tree |
| `/goods` | 商品列表（分类 / 品牌 / 排序 / 分页） | GET /api/goods |
| `/goods/:id` | 商品详情 | GET /api/goods/id/{id}?full=true |
| `/seckill` | 秒杀会场（倒计时 / 抢购） | GET /api/seckills、GET /api/seckills/{id}/goods、POST /api/seckills/grab |
| `/brands` | 品牌馆 | GET /api/brands |
| `/login` | 登录 / 注册 | POST /api/members/login、POST /api/members/register |

## 目录结构

```
src/
├── api/          # 接口层：request.js（Axios 封装）+ 各业务模块 + mock.js
├── stores/       # Pinia：user（登录态）/ cart（购物车）/ favorite（收藏）/ ui（菜单与弹窗）
├── router/       # 路由（hash 模式）
├── components/   # NavBar / HeroCarousel / ProductCard / GooMenu / FloatingTool / GoodsPanel 等
├── views/        # 页面
├── styles/       # 全局主题（CSS Variables）
└── utils/        # reveal.js（滚动入场）
```

## 交互约定

- 游客可浏览全部页面与商品；加购、收藏、秒杀、结算等交易操作需登录（`stores/user.js` 的 `requireLogin()` 统一拦截并跳转登录页带回跳地址）；
- 右下角悬浮工具：点击「+」弹簧展开 购物车 / 收藏 / 回顶；点购物车或收藏弹出大对话框展示对应商品；
- 购物车 / 收藏在 mock 模式本地持久化（localStorage）；联调时购物车可切换 `src/api/cart.js` 网关接口，收藏可按需扩展 `/api/favorites`。

## 说明

- mock 模式演示账号：demo / 123456；
- 商品图片为演示用生成图，无第三方品牌标识；
- 本工程为 PC 商城会员端；后台管理端（admin-web）与后端服务不在本仓库。
