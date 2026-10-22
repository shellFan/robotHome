# PHASE15 NIGHT 5 — BASELINE

**日期**: 2026-10-22  
**分支**: phase15-production-hardening-and-launch  
**起始SHA**: 3a834d5 (Night 4 Release Gate)  
**远程SHA**: 3a834d5 (一致)

---

## 1. 仓库状态

| 检查项 | 结果 |
|--------|------|
| 本地HEAD | 3a834d5 |
| 远程HEAD | 3a834d5 |
| 工作区 | 干净（FILE_CHANGE_FEEDBACK 96文件为IDE行尾符检测，git status无变更） |
| diff origin/main...HEAD | 49 files changed, 4978 insertions(+), 47 deletions(-) |

### Night 1~4 变更范围

| 模块 | 变更 |
|------|------|
| 部署 | nginx.conf三级限流、nginx-https.conf、compose.rehearsal.yml、.env.rehearsal.example |
| 数据库 | FlywayBaselineGuard、docker-compose SQL挂载、crawler_schema |
| 文件存储 | FileModulePolicy私有模块、LocalFileStorageServiceImpl双目录、MinioFileStorageServiceImpl私有隔离、PrivateFileController |
| 采集 | CrawlerEngine增强、GenericWebsiteAdapterTest |
| 商业化 | CommercialScheduler、CommercialSchedulerTest |
| 测试 | FileStorageModuleSecurityTest(+8 MinIO)、FlywayBaselineGuardTest、DatabaseMigrationReliabilityTest |
| 运维 | backup.sh/restore.sh PIPESTATUS修复 |
| 文档 | NIGHT1~4全系列报告 |

---

## 2. 环境可用性

| 环境 | 状态 |
|------|------|
| Docker | **不可用**（未安装） |
| WSL2 | **不可用**（未安装） |
| Java 8 | 可用 |
| Maven | 可用 |
| Node.js | 可用 |
| MySQL 5.6 | **不可用**（无Docker） |
| Nginx | **不可用**（无运行时） |
| MinIO | **不可用**（无Docker） |

**结论**: Runtime验证(MySQL/Nginx/MinIO/性能)保持BLOCKED，排练脚本须可在另一台Docker机器执行。

---

## 3. 风险与优先级

### HIGH优先级（本轮必须修复）

| ID | 风险 | 状态 |
|----|------|------|
| N5-H1 | 私有文件下载仅有private/前缀+bucket校验，缺少业务归属授权——任何登录用户可下载任何企业私有文件 | **修复中**：PrivateFileAccessService业务归属反查 |

### MEDIUM优先级

| ID | 风险 | 计划 |
|----|------|------|
| N5-M1 | MySQL5.6 Flyway运行时验收BLOCKED | 完善排练脚本一键验证 |
| N5-M2 | Nginx运行时验证BLOCKED | 完善可执行验证脚本 |
| N5-M3 | 真实产品采集入库BLOCKED | HTTP解析测试可执行，入库标记BLOCKED |
| N5-M4 | 商业化HTTP E2E BLOCKED | 单元测试覆盖COM-01~10，HTTP层标记BLOCKED |
| N5-M5 | 性能基线BLOCKED | 不虚构指标 |

### LOW优先级

| ID | 风险 |
|----|------|
| N5-L1 | 备份恢复故障注入需Shell自动化测试 |
| N5-L2 | 三端联调需运行时环境 |

---

## 4. Night 5 执行计划

```
Stage 0  ✅ 基线审计（本文件）
Stage 1  ⬜ 环境检查+排练脚本完善
Stage 2  ⬜ MySQL5.6+Flyway验收（排练脚本+BLOCKED标记）
Stage 3  🔄 私有文件攻防+业务归属授权修复（PrivateFileAccessService）
Stage 4  ⬜ Nginx运行时验证（脚本+BLOCKED标记）
Stage 5  ⬜ 真实采集验证（HTTP解析+BLOCKED标记）
Stage 6  ⬜ 商业化E2E（单元测试+BLOCKED标记）
Stage 7  ⬜ 三端联调（契约扫描+BLOCKED标记）
Stage 8  ⬜ 备份恢复故障注入（Shell自动化）
Stage 9  ⬜ 性能基线（BLOCKED）
Stage 10 ⬜ 全量回归+安全复审
Stage 11 ⬜ 最终合并审计
Stage 12 ⬜ 提交推送+最终交付
```

---

## 5. 历史基线确认

| 基线 | Night 4报告 | Night 5重验状态 |
|------|------------|----------------|
| Server测试 | 172/172 | 待重验(Stage 10) |
| Collector测试 | 144/144 | 待重验(Stage 10) |
| Web构建 | PASS | 待重验(Stage 10) |
| Admin构建 | PASS | 待重验(Stage 10) |
| MiniApp结构 | 46页×4文件 | 待重验(Stage 10) |
| BLOCKER | 0 | 待审计 |
| HIGH | 0 | N5-H1新增（修复中） |