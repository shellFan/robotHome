# 部署说明（deploy）

本目录存放机器人之家的 Nginx 等部署配置，配合根目录 `docker-compose.yml` 使用。

## 文件

| 文件 | 说明 |
|------|------|
| `nginx.conf` | 反向代理：`/` PC 前台、`/admin/` 管理后台、`/api/` 与 `/files/` 转发后端 |

## 快速步骤

1. 构建前端（管理后台需 `--base /admin/`）：

   ```bash
   cd robot-home-web && npm i && npm run build
   cd ../robot-home-admin && npm i && npm run build -- --base /admin/
   ```

2. 在仓库根目录启动：

   ```bash
   docker compose up -d --build
   ```

3. 浏览器访问 `http://localhost/`（前台）、`http://localhost/admin/`（后台）。

## 说明

- Compose 内 MySQL / Redis / MinIO 使用本地默认账号，仅适合开发与演示。
- 生产请替换密码、关闭短信 dev 模式，并按需收紧 Swagger 暴露。
- 更多环境与启动方式见根目录 [README.md](../README.md)。
