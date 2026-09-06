# 机器人之家（Robot Home）

机器人行业垂直平台：覆盖机器人产品检索与对比、品牌企业、资讯视频、社区互动，以及 PC 站、管理后台与微信小程序统一后端。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 8、Spring Boot 2.7、MyBatis-Plus、JWT、Springfox/Swagger |
| 数据库 | MySQL 5.6 |
| 缓存 | Redis 7 |
| 文件 | 默认本地磁盘 `./uploads`；可选 MinIO |
| PC 前台 | Vue 3、Vite、Element Plus、Pinia |
| 管理后台 | Vue 3、Vite、Element Plus、ECharts、WangEditor |
| 小程序 | 微信原生小程序（`robot-home-miniapp`） |
| 部署 | Docker Compose、Nginx 反向代理 |

---

## 环境要求

- **JDK 8**、**Maven 3.8+**
- **Node.js 20+**（建议 LTS）、npm
- **MySQL 5.6**（本地或远程）
- **Redis 7**（本地或远程）
- （可选）**MinIO**、**Docker / Docker Compose**
- 小程序开发：**微信开发者工具**

---

## 目录结构

```
robot-home/
├── robot-home-server/     # Spring Boot 后端（端口 8081）
├── robot-home-collector/  # 采集器服务（API Key 认证、内容发布）
├── robot-home-web/        # PC 前台（Vite，开发端口 5173）
├── robot-home-admin/      # 管理后台（Vite，开发端口 3001）
├── robot-home-miniapp/    # 微信小程序（用微信开发者工具导入）
├── sql/                   # 数据库脚本 01 / 02 / 03 / 04
├── docs/                  # API 契约等文档（见 docs/API.md）
├── deploy/                # Nginx 等部署配置
├── scripts/               # 辅助脚本（如建库）
├── docker-compose.yml     # 本地全栈编排
└── README.md
```

---

## 数据库安装

在 MySQL 中按顺序执行：

1. `sql/01_schema.sql` — 建库 `robot_home` 与表结构  
2. `sql/02_init_data.sql` — 基础字典 / 管理员等初始化数据  
3. `sql/03_demo_data.sql` — 演示业务数据（可选）

字符集：`utf8mb4` / `utf8mb4_general_ci`。

也可使用项目内脚本（若已配置 Node 依赖）：

```bash
node scripts/setup-db.js
```

Docker Compose 首次启动时会把上述 SQL 挂载到 MySQL 的 `docker-entrypoint-initdb.d` 自动初始化。

---

## Redis

后端使用 Redis 做登录限流、排行榜等缓存。本地默认：

- Host：`127.0.0.1`
- Port：`6379`
- Password：通过 `SPRING_REDIS_PASSWORD` 环境变量配置

配置项见 `robot-home-server/src/main/resources/application.yml` 中的 `spring.redis.*`。

---

## MinIO（可选）

默认 `file.storage-type: local`，文件落在 `./uploads`，经 `/files/**` 访问。

切换 MinIO 时修改配置（或环境变量）：

```yaml
file:
  storage-type: minio
  minio:
    endpoint: http://localhost:9000
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    bucket-name: robot-home
```

Compose 已包含 MinIO（控制台默认 `http://localhost:9001`）。将后端 `FILE_STORAGE_TYPE` 设为 `minio` 即可联调。

---

## 本地开发启动

### 1. 后端

先确认 MySQL / Redis 可用，并按需修改 `application.yml` 中的数据源与 Redis（也可用环境变量覆盖，无需改文件）。

```bash
cd robot-home-server
mvn spring-boot:run
```

- 服务端口：**8081**
- API 前缀：`http://localhost:8081/api`
- 接口文档：`http://localhost:8081/swagger-ui/index.html` 或 `http://localhost:8081/doc.html`
- API 契约说明：见 [docs/API.md](docs/API.md)

### 2. PC 前台

```bash
cd robot-home-web
npm i
npm run dev
```

开发地址一般为 `http://localhost:5173`，已代理 `/api`、`/files` 到 `8081`。

### 3. 管理后台

```bash
cd robot-home-admin
npm i
npm run dev
```

开发地址一般为 `http://localhost:3001`。

**管理员账号：** 由 `admin.init-username` / `admin.init-password` 配置初始化（首次启动自动创建），请通过环境变量或配置文件设置。

### 4. 微信小程序

1. 安装并打开 **微信开发者工具**
2. 导入项目目录 `robot-home-miniapp`
3. 将请求基址指向后端（如 `http://localhost:8081` 或已配置的合法域名）
4. 开发阶段可在工具中关闭域名校验以便联调

---

## Docker 部署

### 前置：构建前端静态资源

管理后台通过 Nginx 挂在 `/admin/`，构建时需指定 base：

```bash
cd robot-home-web && npm i && npm run build
cd ../robot-home-admin && npm i && npm run build -- --base /admin/
```

### 启动全栈

```bash
# 在 robot-home 根目录
docker compose up -d --build
```

访问：

| 入口 | 地址 |
|------|------|
| PC 前台 | http://localhost/ |
| 管理后台 | http://localhost/admin/ |
| API | http://localhost/api/ |
| 后端直连 | http://localhost:8081/api |
| MinIO 控制台 | http://localhost:9001 |

Compose 凭证通过环境变量配置，详见 `docker-compose.yml`。  
详细说明见 [deploy/README.md](deploy/README.md)。

### 使用远程数据库

可不启 `mysql` 服务，仅保留 redis / backend / nginx，并在 `backend` 环境变量中指向远程库，例如：

```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://<host>:<port>/robot_home?...
SPRING_DATASOURCE_USERNAME: <user>
SPRING_DATASOURCE_PASSWORD: <password>
```

---

## 当前远程环境说明

联调/演示环境信息通过环境变量配置，主要包括：

| 配置项 | 说明 |
|--------|------|
| `SPRING_DATASOURCE_URL` | MySQL 连接地址 |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | 数据库账号（请勿提交真实生产密钥到公开仓库） |
| `SPRING_REDIS_HOST` / `PORT` / `PASSWORD` | Redis 连接信息 |
| `FILE_STORAGE_TYPE` | 文件存储类型（`local` 或 `minio`） |
| `ROBOT_SMS_DEV_MODE` | 开发模式短信验证码直出，生产必须为 `false` |
| `CRAWLER_API_KEY` | 采集器 API Key（生产环境必须配置） |
| `CRAWLER_DEV_MODE` | 采集器开发模式（`true` 跳过认证，生产必须为 `false`） |

**建议：** 本地开发用环境变量或独立 profile 覆盖数据源，避免误连生产；生产密码使用密钥管理，不要写进文档正文。

Spring Boot 常用覆盖示例：

```bash
set SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/robot_home?...
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=<your-password>
set SPRING_REDIS_HOST=127.0.0.1
set SPRING_REDIS_PASSWORD=<your-password>
```

---

## 相关文档

- [docs/API.md](docs/API.md) — API 契约（路径、分页、认证与错误码）
- [deploy/README.md](deploy/README.md) — Nginx / Compose 部署摘要

---

## License

内部项目，版权归项目所有者所有。
