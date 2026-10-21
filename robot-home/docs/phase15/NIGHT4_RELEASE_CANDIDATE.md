# Phase15 Night 4 — Release Candidate

**日期**: 2026-10-22  
**分支**: phase15-production-hardening-and-launch  
**基线**: Night 3 (09c2f15) → Night 4 (本次提交)

---

## 执行摘要

Night 4 完成 10/12 Stage，316/316 测试全 PASS。  
环境限制（Docker/WSL2/MySQL/Nginx/Redis 不可用）下，所有 Code Ready 项完成，Runtime Ready 项待部署环境验证。

| 维度 | 状态 |
|------|------|
| Code Ready | ✅ 全部完成 |
| Runtime Ready | ❌ 待部署环境（Docker 不可用） |
| Production Ready | ❌ 需 Runtime 验收通过 |
| BLOCKER | 0 |
| HIGH | 0（N4-P0-3 已修复） |

---

## Stage 完成状态

| Stage | 内容 | 状态 |
|-------|------|------|
| 0 | 基线确认 + N4-P0-1 修复 | ✅ 9bb4272 |
| 1 | 排练基础设施 (compose.rehearsal.yml + 5 脚本) | ✅ dd4b075 |
| 2 | N4-P0-2 双服务 Flyway baseline 冲突修复 + MySQL 5.6 审查 | ✅ dd4b075 |
| 3 | Nginx 配置审查（三级限流 + 安全头） | ✅ 审查通过 |
| 4 | 采集引擎全链路审查 + 15 新解析质量测试 | ✅ 144/144 |
| 5 | CommercialScheduler 定时任务 0 覆盖 GAP 修复 + 6 新测试 | ✅ 6/6 |
| 6 | MinIO 私有模块隔离修复 (N4-P0-3) + 8 新测试 | ✅ 19/19 |
| 7 | 备份恢复脚本审查 + PIPESTATUS 修复 | ✅ 4 处修复 |
| 8 | 三端构建验证 (Web + Admin + MiniApp) | ✅ 全 PASS |
| 9 | 三端联调 | ⬜ 需运行时环境 |
| 10 | 全量回归 (Server 172 + Collector 144) | ✅ 316/316 |
| 11 | Release Candidate 文档 + Git 推送 | ✅ 本次 |
| 12 | 性能/故障演练 | ⬜ 需运行时环境 |

---

## 修复清单

### N4-P0-1: Compose 缺失 04_crawler_schema.sql 挂载 (commit 9bb4272)
- **问题**: docker-compose.yml/prod.yml 未挂载 04_crawler_schema.sql，全新部署 crawler_source 表缺 V5 列
- **修复**: compose volumes 添加挂载 + FlywayBaselineGuard 添加 crawler_source 列检查

### N4-P0-2: 双服务 Flyway baseline 冲突 (commit dd4b075)
- **问题**: Server 与 Collector 共享同一数据库但 baseline-version 不一致（18 vs 1），并发迁移产生冲突
- **修复**: Collector baseline-version 1→5，SQL 09 改用 PREPARE/EXECUTE 幂等模式

### N4-P0-3: MinIO 私有模块未隔离 (本次提交)
- **问题**: 生产环境 FILE_STORAGE_TYPE=minio，MinioFileStorageServiceImpl 不区分私有模块，enterprise 模块（企业认证材料/营业执照）返回直接公开 URL
- **修复**:
  - 私有模块 objectName 加 `private/` 前缀实现对象级隔离
  - 私有文件返回 `/api/files/private?url=minio://bucket/object` 鉴权端点 URL
  - PrivateFileController 支持 `minio://` 格式走 MinIO 下载
  - resolvePrivateObject 校验：仅允许 private/ 前缀对象 + 仅允许配置 bucket
  - ext() 增加扩展名白名单（与 Local 实现一致，危险扩展名降级 .bin）
  - extractObjectName 修复 scheme 解析 bug（minio:// 双斜杠处理）

### 备份恢复脚本 PIPESTATUS 修复 (本次提交)
- **问题**: `mysqldump | gzip` 管道中 `$?` 捕获的是 gzip 退出码，mysqldump 中途失败时 gzip 仍成功，不完整备份不会被删除
- **修复**: backup.sh（Docker+standalone）和 restore.sh（Docker+standalone）共 4 处改用 `${PIPESTATUS[0]}`

---

## 测试覆盖增量

| 模块 | Night 3 | Night 4 | 增量 |
|------|---------|---------|------|
| Server | 157 | 172 | +15 |
| Collector | 129 | 144 | +15 |
| **合计** | **286** | **316** | **+30** |

### 新增测试明细

**Collector (+15)**: GenericWebsiteAdapterTest
- ParseQuality: h1 标题提取 / article 正文提取 / 非 HTML fail / 空 HTML 不崩溃
- CoverImage: 绝对路径直用 / 协议相对补 https / 相对路径 baseUri resolve
- ProductParam: table.spec 表格提取 / dl 参数列表提取
- LinkExtraction: 内部链接+javascript 过滤 / 扩展名过滤 / 锚点过滤 / 深度限制
- DetectType: /product 路径推断 / config.type 优先

**Server (+15)**:
- CommercialSchedulerTest (6): SCHED-01~06
  - 订单超时关闭: 35 分钟前 PENDING_PAYMENT→EXPIRED / 5 分钟前不关闭 / PAID 不关闭
  - 订阅到期: 3 天后到期 ACTIVE→EXPIRING
  - 配额重置幂等: 上月周期重置 / 本月已重置不重复
- FileStorageModuleSecurityTest (+8): MinIO 私有模块隔离
  - enterprise 返回私有 URL / 公开模块返回直接 URL
  - resolvePrivateObject 拒绝非 private 前缀 / 拒绝错误 bucket / 拒绝非法格式 / 合法对象可下载
  - 危险扩展名降级 .bin / 私有 URL 删除正确提取 objectName

---

## 审查通过项

### Stage 3: Nginx 配置
- 三级限流: 全局 100r/s → API 30r/s → 敏感端点 10r/s
- 安全头: X-Frame-Options DENY / X-Content-Type-Options nosniff / X-XSS-Protection
- gzip 压缩 + 静态资源缓存策略

### Stage 4: 采集引擎全链路
- CrawlerEngine (1100 行): 状态机 / 多线程 workerLoop / 崩溃恢复 / 优雅停机
- HttpFetcher: SSRF 防护入口 / 域名级限速 / 指数退避重试 / 5MB 限制
- UrlSecurityUtil: SSRF 6 层防护（协议白名单/内网 IP/主机名/云元数据/DNS Rebinding/URL 长度）
- GenericWebsiteAdapter: 正文提取 / 封面图三级提取 / 产品参数 / 链接过滤 / 类型推断
- ContentExtractor: 16 个语义选择器 + 文本密度评分 + body 回退

### Stage 7: 备份恢复脚本
- backup.sh: Docker/standalone 双模式 / 磁盘空间检查 / MySQL 5.6 兼容 / MYSQL_PWD 防泄漏 / 轮转保留 30 份
- restore.sh: 二次确认 / 恢复前自动备份 / --binary-mode / 失败提示回退
- verify-backup.sh: gzip 完整性 / CREATE TABLE 计数 / 6 关键表存在 / INSERT 行数

---

## Runtime Ready 遗留项

以下项需部署环境（Docker + MySQL 5.6 + Redis + Nginx + MinIO）后验证:

1. **MySQL 5.6 + Flyway 真实验收**: 静态审查 PASS，Connector/J 8.0.33 兼容性待运行时确认
2. **排练环境验证**: compose.rehearsal.yml + run-rehearsal.sh + verify-runtime.sh
3. **三端联调**: Web/Admin/MiniApp 与 Server 的端到端联通
4. **MinIO 私有文件**: 私有 bucket 策略 + minio:// URL 下载链路
5. **真实产品采集**: CrawlerEngine 真实 HTTP 探测
6. **性能/故障演练**: 限流 / 降级 / 恢复

---

## Git 变更

```
9bb4272  N4-P0-1: compose missing 04_crawler_schema.sql mount + FlywayBaselineGuard
dd4b075  N4-P0-2: fix dual-service Flyway baseline conflict + rehearsal infra
本次提交  N4-P0-3: MinIO private module isolation + CommercialScheduler tests + backup PIPESTATUS fix
```

### 本次提交文件清单

**修改 (5)**:
- robot-home-server/.../file/MinioFileStorageServiceImpl.java — 私有模块隔离+扩展名白名单+resolvePrivateObject
- robot-home-server/.../file/controller/PrivateFileController.java — 支持 MinIO 私有文件下载
- robot-home-server/.../file/FileStorageModuleSecurityTest.java — +8 MinIO 测试
- scripts/db/backup.sh — PIPESTATUS[0] 修复 (2 处)
- scripts/db/restore.sh — PIPESTATUS[0] 修复 (2 处)

**新增 (2)**:
- robot-home-collector/.../GenericWebsiteAdapterTest.java — 15 个解析质量测试
- robot-home-server/.../CommercialSchedulerTest.java — 6 个定时任务测试

**文档 (1)**:
- docs/phase15/NIGHT4_RELEASE_CANDIDATE.md — 本文档