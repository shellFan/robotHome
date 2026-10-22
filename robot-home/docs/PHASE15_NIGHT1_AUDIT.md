# Phase15 Night 1 — 生产部署体系审计报告

> 审计时间：2026-10-20 | 基线 commit：124d102 (Phase14 Final Merge Gate) | 分支：phase15-production-hardening-and-launch

---

## 一、部署拓扑总览

```
                    ┌─────────────┐
                    │   Nginx     │ :80/:443
                    │  (反代+SPA)  │
                    └──────┬──────┘
               ┌───────────┼───────────┐
               ▼           ▼           ▼
        /usr/share/nginx  /api/     /files/
        html/web|admin    ▼           ▼
                    ┌─────────────┐
                    │  Backend    │ :8081 (Server)
                    │ Spring Boot │
                    └──┬───────┬──┘
                       │       │
          ┌────────────┘       └──────────┐
          ▼                                ▼
   ┌─────────────┐                 ┌─────────────┐
   │   MySQL     │ :3306           │   Redis     │ :6379
   │   5.6       │                 │   7.2       │
   └─────────────┘                 └─────────────┘

   ┌─────────────┐    ┌─────────────┐
   │  Collector  │    │   MinIO     │ :9000
   │   :8082     │    │  (对象存储)  │
   └─────────────┘    └─────────────┘
```

**服务清单**：mysql(5.6) / redis(7.2) / minio / backend(Server:8081) / collector(8082) / nginx(1.27)

---

## 二、已有能力盘点

| 能力 | 状态 | 文件 |
|------|------|------|
| 多阶段 Docker 构建 | ✅ | `robot-home-server/Dockerfile` (Maven builder → JRE, 非 root) |
| 本地开发编排 | ✅ | `docker-compose.yml` (6 服务, 健康检查, 依赖条件) |
| 生产编排 | ✅ | `docker-compose.prod.yml` (不暴露端口, .env 强制注入, 资源限制, 日志轮转) |
| Nginx 反代 | ✅ | `deploy/nginx.conf` (SEO/SPA/API/Admin/files 分离) |
| 生产配置外置 | ✅ | `.env.example` (39 行占位符, `${VAR:?}` 强制) |
| Profile 覆盖 | ✅ | `application-prod.yml` (日志/CORS/MinIO) |
| SSRF 防护 | ✅ | `UrlSecurityUtil` (HttpFetcher + CrawlerEngine 重定向校验) |
| Redis 降级 | ✅ | PermissionAspect fail-closed / EntitlementService 直查 DB / NotificationService DB fallback |
| SQL 初始化链 | ✅ | 01~18 按文件名顺序执行 (Night1 修复后) |

---

## 三、Night 1 修复清单

### P0 — 部署阻断缺陷

| # | 缺陷 | 修复 | 影响 |
|---|------|------|------|
| 1 | `docker-compose.yml` 缺 SQL 17/18 挂载 | 添加 `17_phase13_growth_enterprise.sql` + `18_phase14_commercialization.sql` | 全新部署缺 Phase13/14 表 |
| 2 | `docker-compose.prod.yml` 缺 SQL 16/17/18 挂载 | 添加 `16_phase12_data_quality_snapshot.sql` + 17 + 18 | 生产全新部署缺数据质量快照+Phase13/14 表 |

### P1 — 可观测性

| # | 缺陷 | 修复 |
|---|------|------|
| 3 | Server 无 Actuator 依赖，缺健康检查端点 | `pom.xml` 添加 `spring-boot-starter-actuator` |
| 4 | 无 management 端点配置 | `application.yml`: 暴露 health+info, show-details=always; `application-prod.yml`: 仅 health, show-details=never |

### P2 — 备份恢复

| # | 缺陷 | 修复 |
|---|------|------|
| 5 | 无数据库备份脚本 | `scripts/db/backup.sh` (docker/standalone 双模式, gzip 压缩, 自动轮转) |
| 6 | 无数据库恢复脚本 | `scripts/db/restore.sh` (二次确认, --binary-mode) |
| 7 | 无备份验证脚本 | `scripts/db/verify-backup.sh` (gzip 完整性+DDL+关键表+INSERT 统计) |

### P3 — Nginx 加固

| # | 缺陷 | 修复 |
|---|------|------|
| 8 | 无 API 限流 | `limit_req_zone` 10r/s + burst=20 nodelay |
| 9 | Actuator 端点未屏蔽 | 添加 `location ~* ^/actuator { return 404; }` |
| 10 | 静态资源无缓存头 | SPA 静态文件 `expires 30d` + `Cache-Control: public, immutable` |
| 11 | 无 HTTPS 模板 | `deploy/nginx-https.conf` (TLS1.2/1.3, HSTS, HTTP→HTTPS 301) |
| 12 | 版本号泄露 | `server_tokens off` |

### P4 — 版本控制卫生

| # | 缺陷 | 修复 |
|---|------|------|
| 13 | `.gitignore` 未排除 backups/ 和 SSL 证书 | 添加 `backups/`, `*.sql.gz`, `deploy/ssl/` |

---

## 四、安全审计结果

### Redis 故障降级模式

| 组件 | 降级行为 | 安全性 |
|------|----------|--------|
| PermissionAspect | fail-closed（Redis 不可用时返回空权限集，拒绝所有需权限操作） | ✅ 安全优先 |
| EntitlementService | 直查 DB（Redis 不可用时绕过缓存直查权益表） | ✅ fail-safe |
| NotificationService | DB fallback（Redis 不可用时通知落库） | ✅ 不丢失 |

### SSRF 防护

| 校验点 | 实现 |
|--------|------|
| 初始 URL | `UrlSecurityUtil.isAllowedUrl()` 校验协议+内网 IP 黑名单 |
| 重定向目标 | CrawlerEngine 重定向后二次校验 |
| 拒绝原因 | `getRejectionReason()` 记录日志 |

### 生产环境安全实践

- ✅ 非 root 容器（appuser）
- ✅ 端口不暴露（仅 Nginx 对外）
- ✅ 敏感配置 `.env` 强制注入（`${VAR:?}` 模式）
- ✅ Swagger/Actuator Nginx 层屏蔽
- ✅ 健康检查端点生产 show-details=never
- ✅ JWT Secret / DB 密码 / MinIO 密钥全部环境变量化

---

## 五、环境阻塞记录

| 阻塞项 | 原因 | 影响 | 状态 |
|--------|------|------|------|
| MYSQL56_RUNTIME_BLOCKED | Docker 不可用（本机未安装 Docker Desktop） | 无法执行 MySQL 5.6 运行时兼容性实测 | ⛔ BLOCKED |
| NGINX_RUNTIME_BLOCKED | 同上 | 无法执行 nginx -t + 端到端反代验证 | ⛔ BLOCKED |
| WSL2_BLOCKED | WSL2 未安装 | 无法在 Windows 上运行 bash 部署脚本 | ⛔ BLOCKED |

**缓解措施**：静态审计已完成全部配置文件审查；运行时验证待具备 Docker 环境后补充。

---

## 六、MySQL 5.6 生命周期风险

MySQL 5.6 已于 **2021-02** 结束官方扩展维护，不再接收安全补丁。

**当前决策**：维持 5.6（项目技术栈锁定，且 Docker 镜像 `mysql:5.6` 仍可拉取）。

**升级建议**（Phase16+ 规划）：
1. 升级路径：5.6 → 5.7 → 8.0（不可跨大版本直接升级）
2. 兼容性风险：`utf8mb4` 索引长度（5.6 需 `varchar(191)` 限制，5.7+ 无此限制）
3. 驱动：`mysql-connector-j 8.0.33` 已兼容 5.6/5.7/8.0
4. 建议先在 staging 环境执行 `mysql_upgrade` 演练

---

## 七、待办优先级

| 优先级 | 事项 | 依赖 |
|--------|------|------|
| P0 | ~~compose SQL 挂载修复~~ | ✅ 已完成 |
| P1 | ~~Actuator 健康检查~~ | ✅ 已完成 |
| P1 | ~~备份恢复脚本~~ | ✅ 已完成 |
| P2 | MySQL 5.6 运行时实测 | Docker 环境 |
| P2 | Nginx 运行时验证 (nginx -t) | Docker 环境 |
| P2 | staging 环境配置 (application-staging.yml) | 无 |
| P3 | Redis 降级集成测试（模拟 Redis 不可用） | 无 |
| P3 | 全链路冒烟测试（Docker 环境端到端） | Docker 环境 |

---

## 八、回归测试基线

Phase14 Final Merge Gate 基线：**124/124 PASS**（Server 110 + 安全回归 5 + E2E 3 + 订阅生命周期 14 → 合并去重后 124）

Night 1 变更影响面：
- `pom.xml` 新增 actuator 依赖 → 需全量回归验证无依赖冲突
- `application.yml` 新增 management 配置 → 需验证测试上下文加载正常
- compose/nginx/scripts → 不影响 Java 测试，影响部署

**回归策略**：`mvn clean test` 全量执行，预期 124/124 PASS。