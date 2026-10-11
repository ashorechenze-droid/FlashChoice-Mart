# FlashChoice-Mart

基于 Spring Cloud 的 B2C 微服务秒杀商城，包含 Vue 3 前端、商品与品牌管理、购物车、订单、用户和秒杀模块。

## 项目目录

- `frontend/`：商城前端，包含品牌馆使用的本地 Logo 图片。
- `FCM-gateway/`、`FCM-microservice/`、`FCM-common/`、`FCM-spi/`：后端网关、微服务及公共模块。
- `sql/`：数据库脚本；`docs/`：接口和项目文档。
- `start-project.bat`、`start-project.ps1`：Windows 一键启动脚本。

## 本地运行

先安装并配置 JDK 25、Node.js、MySQL、Redis、RabbitMQ（配套 Erlang 27）和 Nacos，再按 `sql/` 与 `docs/` 中的说明初始化数据库。启动脚本中的依赖路径按本机安装位置设置，必要时可通过 PowerShell 参数覆盖。

在 Windows 上双击 `start-project.bat`。脚本会构建后端、安装前端依赖（首次启动时）并启动服务。启动后访问 [商城](http://localhost:5173)；网关地址为 `http://localhost:8888`。

也可单独启动前端：

```powershell
cd frontend
npm ci
npm run dev
```
