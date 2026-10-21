# Phase15 Night 3 — 代码审计报告

> 审计时间：2026-10-21 | 基线 commit：009ab07 | 分支：phase15-production-hardening-and-launch
> 审计范围：Night 1 + Night 2 全部新增/修改代码

---

## 一、审计结论

| 级别 | 数量 | 状态 |
|------|------|------|
| BLOCKER | 1 | 待修复 |
| HIGH | 4 | 待修复 |
| MEDIUM | 3 | 待修复 |
| LOW | 2 | 记录 |

---

## 二、BLOCKER

### BLOCKER-1: Flyway baseline-on-migrate 无 Schema 指纹校验
- **文件**: `application.yml` (spring.flyway) + `DatabaseMigrationReliabilityTest`
- **问题**: `baseline-on-migrate: true` + `baseline-version: "18"` 会对**任意非空数据库**无条件 baseline 到 V18。若数据库仅有 Phase1-10 的表（缺少 Phase13/14 的 enterprise_plan/enterprise_subscription/advertising_slot 等表），Flyway 仍会 baseline 到 V18 并标记"已最新"，缺失的表**永远不会被创建**。
- **风险**: 历史不完整库升级后商业化功能完全缺失，且无任何报错。
- **修复**: 新增 `FlywayBaselineGuard` — 在 Flyway migrate 前校验必需表/列指纹，不满足则拒绝启动。

---

## 三、HIGH

### HIGH-1: 私有文件降级到公开本地目录
- **文件**: `FileStorageRouter.java` + `LocalFileStorageServiceImpl.java` + `nginx.conf`
- **问题**: `fallback-to-local=true` 对**所有文件**生效，包括企业认证材料/营业执照/采购合同等私有文件。本地存储通过 `/files/` 公开访问（Nginx 无鉴权代理），MinIO 故障时私有文件暴露。
- **修复**: 新增文件可见性分类（PUBLIC/PRIVATE），私有文件 MinIO 不可用时拒绝写入而非降级公开目录。

### HIGH-2: 本地存储无文件扩展名白名单
- **文件**: `LocalFileStorageServiceImpl.ext()`
- **问题**: 接受任意扩展名（.exe/.jsp/.html/.svg），.html/.svg 可导致存储型 XSS。
- **修复**: 扩展名白名单（图片+文档），拒绝危险扩展名。

### HIGH-3: restore.sh BACKUP_DIR 未定义
- **文件**: `scripts/db/restore.sh` L76/L104
- **问题**: `PRE_RESTORE_BACKUP="${BACKUP_DIR}/pre_restore_..."` 使用了从未定义的 `BACKUP_DIR` 变量，恢复前备份写入错误路径（根目录 `/pre_restore_*`），导致权限拒绝或备份丢失。
- **修复**: 在 restore.sh 中定义 `BACKUP_DIR`。

### HIGH-4: Nginx 10r/s 统一限流策略
- **文件**: `deploy/nginx.conf`
- **问题**: 所有 `/api/` 共享 10r/s 限流。支付回调可能被限流丢弃；正常浏览被突发流量挤占。
- **修复**: 按接口风险分级限流（支付回调不限流/登录严格/普通查询宽松）。

---

## 四、MEDIUM

### MEDIUM-1: backup.sh set -e 导致失败清理代码不可达
- **文件**: `scripts/db/backup.sh`
- **问题**: `set -euo pipefail` 下 mysqldump 管道失败时脚本立即退出，`DUMP_EXIT=$?` 及 `rm -f` 清理不可达，不完整备份文件残留。
- **修复**: 管道后接 `|| true` 使脚本继续到清理逻辑。

### MEDIUM-2: LocalFileStorageServiceImpl.delete() 路径穿越
- **问题**: delete() 从 URL 提取 key 后直接 `new File(uploadDir, key)`，key 含 `../` 可逃逸上传目录删除任意文件。
- **修复**: canonical path 校验，确保删除目标在上传目录内。

### MEDIUM-3: DB测试缺少不完整Schema拒绝baseline场景
- **问题**: DB-02 用最小 schema（robot+brand）验证 baseline，但未验证**缺少 Phase13/14 表时拒绝 baseline**。
- **修复**: 新增 DB-12 测试。

---

## 五、LOW

### LOW-1: Nginx /files/ 无限流
- /files/ 位置未配置 limit_req，可被大量下载消耗带宽。修复时一并处理。

### LOW-2: schema_audit_log INSERT IGNORE 在 H2 MODE=MySQL 下的兼容性
- H2 对 INSERT IGNORE 支持有限，当前测试通过但语义依赖 MySQL 方言。记录待观察。

---

## 六、合规项确认（PASS）

1. ✅ MockSandboxPaymentProvider.isAvailable() prod 强制 false
2. ✅ EntitlementService.expireEntitlements 按 subscriptionId 作用域撤销
3. ✅ CommercialSubscriptionServiceImpl.activateFromOrder 幂等（sourceOrderId）
4. ✅ V19 SQL CREATE TABLE IF NOT EXISTS + INSERT IGNORE 幂等
5. ✅ Flyway validate-on-migrate + fail-fast
6. ✅ backup.sh pipefail + MYSQL_PWD + 磁盘检查 + 轮转
7. ✅ restore.sh 二次确认 + 恢复前备份 + --binary-mode
8. ✅ Nginx server_tokens off + 安全响应头 + Swagger/Actuator 屏蔽
9. ✅ docker-compose.prod.yml 端口不暴露 + .env 强制注入
10. ✅ PermissionAspect Redis fail-closed
11. ✅ extractProductFields 三重匹配（型号/价格/分类/日期/链接）

---

## 七、修复优先级

| 优先级 | 项 | Stage |
|--------|-----|-------|
| P0 | BLOCKER-1 Flyway baseline guard | Stage 1 |
| P0 | HIGH-1 私有文件隔离 | Stage 2 |
| P0 | HIGH-2 扩展名白名单 | Stage 2 |
| P1 | HIGH-3 restore.sh BACKUP_DIR | Stage 5 |
| P1 | HIGH-4 Nginx 分级限流 | Stage 6 |
| P1 | MEDIUM-1 backup.sh 清理不可达 | Stage 5 |
| P1 | MEDIUM-2 delete 路径穿越 | Stage 2 |
| P2 | MEDIUM-3 DB-12 测试 | Stage 1 |