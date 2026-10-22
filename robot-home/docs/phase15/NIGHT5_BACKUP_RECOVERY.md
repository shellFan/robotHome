# Phase15 Night 5 — 备份恢复故障注入测试报告

> Stage 8 | 日期: 2026-10-22 | 验证等级: UNIT_TEST_PASS (Shell)

## 1. 测试概要

| 项目 | 值 |
|------|-----|
| 测试脚本 | `scripts/phase15/test-backup-recovery.sh` |
| 检查项总数 | 27 |
| PASS | 27 |
| FAIL | 0 |
| 验证等级 | UNIT_TEST_PASS（静态分析+Mock故障注入） |

## 2. 测试模块与结果

### 模块1: PIPESTATUS静态检查 (4项)
| 检查项 | 结果 |
|--------|------|
| backup.sh 包含PIPESTATUS[0] (4处) | PASS |
| restore.sh 包含PIPESTATUS[0] (4处) | PASS |
| backup.sh PIPESTATUS赋值DUMP_EXIT (2处) | PASS |
| backup.sh DUMP_EXIT非零时删除不完整备份 (2处) | PASS |

**核心验证**：mysqldump|gzip管道中，`$?`只捕获gzip退出码（总是0），必须用`${PIPESTATUS[0]}`捕获mysqldump退出码。backup.sh在Docker模式(行76-77)和standalone模式(行114-115)均正确使用。

### 模块2: 错误处理静态检查 (8项)
| 检查项 | 结果 |
|--------|------|
| backup.sh set -euo pipefail | PASS |
| restore.sh set -euo pipefail | PASS |
| backup.sh 磁盘空间检查 | PASS |
| restore.sh 二次确认(YES) | PASS |
| restore.sh 恢复前自动备份 | PASS |
| restore.sh BACKUP_DIR已定义 | PASS |
| backup.sh MYSQL_PWD环境变量(防泄漏) | PASS |
| backup.sh 备份轮转保留 | PASS |

### 模块3: Mock故障注入 (2项)
| 检查项 | 期望 | 结果 |
|--------|------|------|
| PIPESTATUS[0]捕获管道首命令退出码 | 1 | PASS |
| 无pipefail时$?捕获最后命令退出码 | 0 | PASS |

**故障注入验证**：`false | true`场景下，`$?=0`（旧bug根因）而`${PIPESTATUS[0]}=1`（正确捕获），证明backup.sh使用PIPESTATUS是必要修复。

### 模块4: 空备份检测 (4项)
| 检查项 | 结果 |
|--------|------|
| 空备份文件大小>0但解压后为空(gzip头) | PASS |
| verify-backup.sh 存在 | PASS |
| verify-backup.sh gzip完整性检查 | PASS |
| verify-backup.sh CREATE TABLE计数检查 | PASS |

### 模块5: 损坏文件检测 (2项)
| 检查项 | 结果 |
|--------|------|
| gzip -t检测截断损坏文件 (69B→34B) | PASS |
| 正常gzip文件通过完整性检查 | PASS |

**损坏注入方式**：将合法gzip文件截断至50%大小，产生CRC校验失败，gzip -t可靠检测。

### 模块6: 脚本结构验证 (7项)
| 检查项 | 结果 |
|--------|------|
| backup.sh Docker模式支持 | PASS |
| backup.sh standalone模式支持 | PASS |
| restore.sh Docker模式支持 | PASS |
| restore.sh --binary-mode | PASS |
| backup.sh bash语法检查 | PASS |
| restore.sh bash语法检查 | PASS |
| verify-backup.sh bash语法检查 | PASS |

## 3. 验证等级说明
- **UNIT_TEST_PASS**: 27/27静态分析+Mock故障注入通过
- **INTEGRATION_TEST_PASS**: 需要MySQL实例执行真实备份/恢复（Docker不可用，BLOCKED）
- **RUNTIME_PASS**: 需要完整排练环境执行端到端备份→损坏→恢复流程（BLOCKED）

## 4. 排练环境执行指南
在有Docker的机器上执行完整验证：
```bash
# 1. 启动排练环境
./scripts/phase15/run-rehearsal.sh

# 2. 执行真实备份
docker exec robot-mysql-primary /opt/scripts/backup.sh

# 3. 验证备份完整性
docker exec robot-mysql-primary /opt/scripts/verify-backup.sh

# 4. 执行故障恢复演练（删除数据库→恢复→验证）
docker exec robot-mysql-primary /opt/scripts/restore.sh

# 5. 清理排练环境
./scripts/phase15/cleanup-rehearsal.sh
```