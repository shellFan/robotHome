# PHASE15 NIGHT 4 — BASELINE

**日期**: 2026-10-21  
**分支**: phase15-production-hardening-and-launch  
**基线SHA**: 09c2f155608c8b4c6f5701156c5d04f376301f48

---

## 1. Git状态

- 本地HEAD = 远程 = 09c2f15
- 工作区干净（Night 3已全部提交推送）

## 2. 项目模块路径

| 模块 | 路径 | 构建工具 |
|------|------|----------|
| Server | robot-home/robot-home-server | Maven (Java 8, Spring Boot 2.x) |
| Collector | robot-home/robot-home-collector | Maven (Java 8) |
| Web | robot-home/robot-home-web | Vite + Vue 3 |
| Admin | robot-home/robot-home-admin | Vite + Vue 3 |
| MiniApp | robot-home/robot-home-miniapp | 微信小程序原生 |
| SQL | robot-home/sql/ | 01-18 初始化链 + V19 Flyway迁移 |
| 部署 | robot-home/deploy/ | nginx.conf, nginx-https.conf |
| 脚本 | robot-home/scripts/db/ | backup.sh, restore.sh, verify-backup.sh |
| Compose | robot-home/docker-compose.yml, docker-compose.prod.yml | MySQL 5.6 + Redis 7.2 + MinIO + Nginx |

## 3. 运行环境检测

| 工具 | 状态 |
|------|------|
| Docker | NOT AVAILABLE |
| WSL2 | NOT AVAILABLE |
| MySQL客户端 | NOT AVAILABLE |
| Nginx | NOT AVAILABLE |
| Redis | NOT AVAILABLE |
| Java | 1.8.0_212 AVAILABLE |
| Maven | 3.3.9 AVAILABLE |
| Node | v20.20.1 AVAILABLE |
| npm | 10.8.2 AVAILABLE |

## 4. Night 1-3已完成能力

- **Night 1**: Compose SQL挂载修复, Actuator健康检查, Springfox兼容, 备份恢复脚本, Nginx HTTPS模板, 部署安全加固
- **Night 2**: Flyway 8.5.13集成, V19迁移, baseline-version=18, 备份恢复加固, 机器人结构化字段提取, MinIO本地降级, Redis故障一致性, 商业化并发测试, API契约修复
- **Night 3**: FlywayBaselineGuard(12表指纹校验), 企业私有文件隔离, PrivateFileController鉴权下载, 非法扩展名兜底, restore.sh修复, Nginx三级限流, 存储路径安全, 157/157 PASS

## 5. Night 4新发现P0问题

**N4-P0-1: docker-compose.yml和docker-compose.prod.yml均未挂载04_crawler_schema.sql**
- 影响：全新部署缺少14张采集系统表(crawler_source/task/url/page/article/product/media/error/match_record/rule/log/product_change/brand_alias/param_mapping)
- 后果：Collector服务完全不可用，09_crawler_sources.sql因crawler_source表不存在而失败
- 修复：两个compose文件添加04_crawler_schema.sql挂载（在04_migration_source_url.sql之前）
- 加固：FlywayBaselineGuard REQUIRED_CORE_TABLES添加crawler_source，新增DB-16测试

## 6. 尚未验证的能力

| 能力 | 状态 | 原因 |
|------|------|------|
| MySQL 5.6 Fresh部署 | BLOCKED | Docker/MySQL不可用 |
| MySQL 5.6 Upgrade部署 | BLOCKED | Docker/MySQL不可用 |
| Flyway真实生命周期验证 | BLOCKED | 需MySQL运行时 |
| Nginx配置加载验证 | BLOCKED | Nginx不可用 |
| 真实产品采集入库 | BLOCKED | 需隔离数据库 |
| 商业化HTTP E2E | BLOCKED | 需完整运行环境 |
| MinIO私有文件运行时 | BLOCKED | Docker不可用 |
| 备份恢复真实验证 | BLOCKED | MySQL不可用 |
| 性能压测 | BLOCKED | 需完整运行环境 |

## 7. 可执行的验收项目

| 项目 | 方式 |
|------|------|
| Server全量回归 | mvn clean test |
| Collector全量回归 | mvn clean test |
| Web构建 | npm run build |
| Admin构建 | npm run build |
| MiniApp静态检查 | 代码审查+app.json验证 |
| Flyway守卫单元测试 | H2内存库 |
| 商业化集成测试 | Spring Boot Test + H2 |
| 安全审计 | 代码审查 |
| API契约验证 | 前后端路径交叉 |
| Nginx配置审查 | 静态语法/逻辑审查 |
| Compose配置审查 | YAML结构+挂载完整性 |
| 排练脚本 | 创建可一键执行的隔离验收脚本 |

## 8. 本轮P0/P1/P2

- **P0**: 修复compose缺失04_crawler_schema.sql挂载（已完成）
- **P0**: FlywayBaselineGuard增加crawler_source检查（已完成）
- **P1**: 创建排练脚本和rehearsal compose供Docker可用时一键验证
- **P1**: MySQL 5.6兼容性代码审查（Flyway 8.5.13 + MySQL 5.6 + utf8mb4）
- **P1**: 商业化E2E集成测试补强
- **P1**: 真实HTTP采集解析验证（无数据库）
- **P2**: Nginx配置深度审查
- **P2**: 备份恢复脚本加固
- **P2**: 三端构建验证