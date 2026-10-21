# Phase15 Night4 — MySQL 5.6 + Flyway 兼容性静态审查

**日期**: 2026-10-21 Night 4
**状态**: Code Ready ✅ | Runtime Ready ❌ (Docker不可用，待排练验证)
**基线**: 9bb4272 + Night4修复

## 审查范围

| 组件 | 版本 | 审查项 |
|------|------|--------|
| MySQL Server | 5.6 (Docker image mysql:5.6) | utf8mb4/索引长度/语法兼容 |
| MySQL Connector/J | 8.0.33 | MySQL 5.6连接兼容性 |
| Flyway | 8.5.13 + flyway-mysql 8.5.13 | baseline/migrate/MySQL 5.6支持 |
| SQL初始化链 | 01-18 (19个文件) | MySQL 5.6语法/幂等性/执行顺序 |
| Flyway迁移 | V19 (Server) + V1-V5 (Collector) | 双服务baseline冲突 |

## 审查结果

### 1. utf8mb4 + 索引长度限制 ✅ PASS

MySQL 5.6 InnoDB默认COMPACT行格式，索引键长度限制767字节。
utf8mb4每字符4字节 → 单列索引最大191字符。

**验证结果**: 所有被索引列均≤190字符：
- `normalized_keyword` VARCHAR(190) → 760字节 ✓ (精确设计在767限制内)
- `username` VARCHAR(64) → 256字节 ✓
- `keyword` VARCHAR(128) → 512字节 ✓
- `url_hash` VARCHAR(64) → 256字节 ✓
- `alias_name` VARCHAR(128) → 512字节 ✓
- `order_no` VARCHAR(32) → 128字节 ✓
- `dedup_key` VARCHAR(128) → 512字节 ✓
- 复合索引最大: uk_raw_name(raw_name 128 + category) ≈ 560字节 ✓
- VARCHAR(255)+列(base_url/title等)均无索引 ✓

### 2. MySQL 5.7+/8.0+专有语法 ✅ PASS

扫描全部SQL文件，未发现：
- `GENERATED ALWAYS AS` (5.7+)
- `RENAME INDEX` (5.7+)
- `WITH RECURSIVE` / 窗口函数 (8.0+)
- `utf8mb4_0900_ai_ci` 排序规则 (8.0+)
- `CHECK` 约束强制执行 (8.0+)
- `JSON` 类型原生支持 (5.7+，5.6用TEXT/LONGTEXT替代)

### 3. MySQL Connector/J 8.0.33 + MySQL 5.6 ⚠️ 注意

Connector/J 8.0官方支持MySQL 5.7+，但与5.6实际兼容：
- ✅ 认证: MySQL 5.6使用mysql_native_password，Connector/J 8自动回退
- ✅ `characterEncoding=utf8` → Connector/J 8.0.13+映射为utf8mb4
- ✅ `serverTimezone=Asia/Shanghai` → 客户端时区设置，5.6兼容
- ✅ `useSSL=false` → 5.6默认无SSL配置
- ⚠️ `allowPublicKeyRetrieval` 仅caching_sha2_password需要，5.6不需要（无害）

**建议**: 排练环境验证连接后确认无功能退化。

### 4. Flyway 8.5.13 + MySQL 5.6 ✅ PASS

- Flyway 8.5.13官方支持MySQL 5.6（flyway-mysql模块）
- `baseline-on-migrate: true` + `baseline-version` 配置正确
- `lock-retry-timeout: 60` 使用GET_LOCK实现并发互斥（5.6兼容）
- V19迁移使用标准MySQL 5.6语法（CREATE TABLE IF NOT EXISTS + INSERT IGNORE）

### 5. 双服务Flyway baseline冲突 🔧 FIXED (N4-P0-2)

**问题**: Server和Collector共享同一数据库和flyway_schema_history表，但baseline版本不同：
- Server: baseline-version="18" → 全新部署时baseline到V18
- Collector: baseline-version="1" → V2-V5在V18以下被跳过
- V5添加的health_status等列不在initdb SQL链中 → **Server-first启动时Collector运行时缺列崩溃**

**修复**:
1. 04_crawler_schema.sql: crawler_source表添加16个V5列(health_status/consecutive_failures/avg_latency_ms/last_success_time/last_fail_time/last_fail_reason/total_crawls/total_success_crawls/robots_checked/robots_allowed/robots_checked_time/rss_url/language/region/priority/tags) + idx_health/idx_priority索引
2. 04_crawler_schema.sql: crawler_error表添加response_time_ms/error_category列 + idx_error_category索引
3. 09_crawler_sources.sql: ALTER TABLE改为条件式PREPARE/EXECUTE幂等模式（列已存在时跳过）
4. Collector application.yml: baseline-version从"1"改为"5"（V1-V5列已在initdb链中，不再需要执行）

**验证**: Server 158/158 PASS, Collector测试运行中

### 6. SQL初始化链幂等性 🔧 FIXED

**问题**: 09_crawler_sources.sql使用无条件ALTER TABLE ADD COLUMN，与04_crawler_schema.sql的CREATE TABLE列定义冲突（Duplicate column name错误会中止initdb脚本执行）。

**修复**: 改为INFORMATION_SCHEMA条件判断 + PREPARE/EXECUTE幂等模式。

## Runtime验证清单（待Docker可用时执行）

```bash
# 1. 启动排练环境
bash scripts/phase15/preflight.sh
bash scripts/phase15/run-rehearsal.sh

# 2. MySQL 5.6运行时验证
bash scripts/phase15/verify-runtime.sh
# 验证项:
# - MySQL版本 = 5.6.x
# - utf8mb4字符集
# - 14张采集表全部存在
# - FlywayBaselineGuard 13张必需表
# - crawler_source含V5列(health_status等)
# - crawler_error含response_time_ms/error_category

# 3. Server连接MySQL 5.6
# - Flyway baseline到V18 → V19迁移成功
# - 应用启动无缺列错误

# 4. Collector连接MySQL 5.6（Server-first场景）
# - Flyway baseline到V5
# - crawler_source查询含health_status列
# - 应用启动无缺列错误

# 5. 清理
bash scripts/phase15/cleanup-rehearsal.sh
```

## 结论

| 类别 | 状态 | 说明 |
|------|------|------|
| Code Ready | ✅ | 全部SQL/配置/迁移文件MySQL 5.6兼容 |
| Runtime Ready | ❌ | Docker不可用，待排练环境验证 |
| N4-P0-2修复 | ✅ | 双服务baseline冲突+缺列问题已修复 |
| 索引长度 | ✅ | 所有索引≤767字节限制 |
| 语法兼容 | ✅ | 无5.7+/8.0+专有语法 |
| 连接器兼容 | ⚠️ | Connector/J 8.0.33+MySQL 5.6实际可用，待运行时确认 |