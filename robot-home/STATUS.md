# 机器人之家 — 开发进度检查点

> 最后更新：2026-09-04。**第一阶段验收通过，安全加固完成。**

## 本地启动

```powershell
# 后端（若未启动）
cd robot-home-server
java -jar target/robot-home-server.jar

# PC 开发站
cd ../robot-home-web
npm run dev          # 默认 http://localhost:5173 ，API 代理到 8081

# 管理后台
cd ../robot-home-admin
npm run dev          # 默认 http://localhost:5174 或 vite 提示端口

# 采集器
cd ../robot-home-collector
mvn spring-boot:run  # 需要 CRAWLER_API_KEY 环境变量（或 dev-mode）

# 小程序：微信开发者工具打开 robot-home-miniapp，配置 request 合法域名为后端地址
```

| 入口 | 地址 |
|------|------|
| API | http://localhost:8081/api |
| Swagger | http://localhost:8081/swagger-ui/index.html |
| 后台账号 | 由 `admin.init-username` / `admin.init-password` 配置（首次启动自动初始化） |

## 环境

| 项 | 地址 | 状态 |
|----|------|------|
| MySQL | 由环境变量 `SPRING_DATASOURCE_*` 配置 | ✅ 已建库并导入 schema + init + demo（24 机器人、12 品牌、22 资讯） |
| Redis | 由环境变量 `SPRING_REDIS_*` 配置 | ✅ 读写正常 |
| 后端配置 | `robot-home-server/src/main/resources/application.yml` | ✅ 敏感信息已环境变量化 |

## 模块完成度

| 模块 | 状态 | 说明 |
|------|------|------|
| SQL 三件套 | ✅ | `sql/01_schema.sql` 46 表 + init + demo |
| 后端 API | ✅ | 37 Controller、27 Service、18 集成测试 |
| 采集器 | ✅ | API Key 双模式认证、发布审核、关联表全量写入、14 个单元测试 |
| PC Web 页面 | ✅ | 35 个 vue（路由 30 条全覆盖） |
| Admin 页面 | ✅ | 36 个 vue（菜单 CRUD 全覆盖） |
| 小程序 | ✅ | `robot-home-miniapp` 138 文件 |
| README | ✅ | 根目录已有 |
| docker-compose | ✅ | 已有 |
| Dockerfile / nginx.conf | ✅ | `robot-home-server/Dockerfile`、`deploy/nginx.conf` |

## 安全加固记录（2026-09-04）

| 项 | 修复内容 |
|----|----------|
| 采集器鉴权 | API Key 拦截器重构为 dev-mode/production 双模式；生产环境未配置 api-key 启动失败；禁止 URL 参数传递密钥 |
| 发布审核 | 仅 AUTO_APPROVED 状态可发布，PENDING_REVIEW 需人工审核 |
| 产品发布链路 | 主表 + robot_image + robot_price + robot_tag 关联表全量写入 |
| 文章发布链路 | 重复发布执行更新而非插入；分类匹配可扩展 |
| 敏感信息清理 | setup-db.js 凭据环境变量化；admin 登录页移除预填；sms-dev-mode 默认 false |
| 测试覆盖 | 采集器 14 个单元测试（鉴权 7 + 适配器 3 + 发布规则 2 + 幂等 2） |
| Mapper 扫描 | 移除重复 @MapperScan，仅保留 MybatisPlusConfig 声明 |
| Docker Compose | CRAWLER_API_KEY 非空默认值；增加 CRAWLER_DEV_MODE=false |

## 待验证

- [x] `mvn clean package` 后端构建
- [x] `npm run build` PC Web
- [x] `npm run build` Admin
- [x] 后端连远程 MySQL 启动 + 核心 API 冒烟
- [x] Redis 写入正常
- [x] 集成测试 `mvn test`

## 恢复命令

```powershell
# 1. 验库
cd robot-home
node scripts/setup-db.js   # 需设置 MYSQL_HOST/PORT/USER/PASSWORD 环境变量

# 2. 构建
cd robot-home-server && mvn clean package -DskipTests
cd ../robot-home-web && npm install && npm run build
cd ../robot-home-admin && npm install && npm run build

# 3. 启动后端
cd ../robot-home-server
java -jar target/robot-home-server-*.jar
```
