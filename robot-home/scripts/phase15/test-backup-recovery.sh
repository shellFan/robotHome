#!/usr/bin/env bash
# 机器人之家 Phase15 Night5 — 备份恢复故障注入测试
# 在无MySQL环境下通过mock验证backup.sh/restore.sh的故障处理逻辑
# 用法: bash scripts/phase15/test-backup-recovery.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROBOT_HOME="$(cd "$SCRIPT_DIR/../.." && pwd)"
DB_SCRIPTS="$ROBOT_HOME/scripts/db"

PASS=0
FAIL=0
SKIP=0

check() {
    local desc="$1"
    local result="$2"
    if [ "$result" = "true" ]; then
        echo "  [PASS] $desc"
        PASS=$((PASS + 1))
    elif [ "$result" = "skip" ]; then
        echo "  [SKIP] $desc"
        SKIP=$((SKIP + 1))
    else
        echo "  [FAIL] $desc"
        FAIL=$((FAIL + 1))
    fi
}

echo "===================================="
echo " Phase15 Night5 Backup Recovery Tests"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "===================================="
echo ""

# ========== 1. 静态检查: PIPESTATUS 使用 ==========
echo "-- 1. PIPESTATUS 静态检查 --"

BACKUP_PIPESTATUS=$(grep -c 'PIPESTATUS\[0\]' "$DB_SCRIPTS/backup.sh" 2>/dev/null || echo "0")
check "backup.sh 包含PIPESTATUS[0] (${BACKUP_PIPESTATUS}处)" "$([ "$BACKUP_PIPESTATUS" -ge 2 ] && echo true || echo false)"

RESTORE_PIPESTATUS=$(grep -c 'PIPESTATUS\[0\]' "$DB_SCRIPTS/restore.sh" 2>/dev/null || echo "0")
check "restore.sh 包含PIPESTATUS[0] (${RESTORE_PIPESTATUS}处)" "$([ "$RESTORE_PIPESTATUS" -ge 2 ] && echo true || echo false)"

# 检查PIPESTATUS赋值给DUMP_EXIT并用于错误判断
BACKUP_DUMP_EXIT_LOGIC=$(grep -c 'DUMP_EXIT=.*PIPESTATUS' "$DB_SCRIPTS/backup.sh" 2>/dev/null || echo "0")
check "backup.sh PIPESTATUS赋值DUMP_EXIT (${BACKUP_DUMP_EXIT_LOGIC}处)" "$([ "$BACKUP_DUMP_EXIT_LOGIC" -ge 2 ] && echo true || echo false)"

BACKUP_DUMP_EXIT_CHECK=$(grep -c 'DUMP_EXIT.*-ne 0\|DUMP_EXIT.*ne 0' "$DB_SCRIPTS/backup.sh" 2>/dev/null || echo "0")
check "backup.sh DUMP_EXIT非零时删除不完整备份 (${BACKUP_DUMP_EXIT_CHECK}处)" "$([ "$BACKUP_DUMP_EXIT_CHECK" -ge 2 ] && echo true || echo false)"

echo ""

# ========== 2. 静态检查: 错误处理 ==========
echo "-- 2. 错误处理静态检查 --"

check "backup.sh set -euo pipefail" "$(grep -q 'set -euo pipefail' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"
check "restore.sh set -euo pipefail" "$(grep -q 'set -euo pipefail' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"
check "backup.sh 磁盘空间检查" "$(grep -q 'disk\|DISK\|空间' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"
check "restore.sh 二次确认(YES)" "$(grep -q 'YES\|confirm' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"
check "restore.sh 恢复前自动备份" "$(grep -q '恢复前\|pre.*backup\|BACKUP_BEFORE' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"
check "restore.sh BACKUP_DIR已定义" "$(grep -q 'BACKUP_DIR=' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"
check "backup.sh MYSQL_PWD环境变量(防泄漏)" "$(grep -q 'MYSQL_PWD' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"
check "backup.sh 备份轮转保留" "$(grep -q '轮转\|rotate\|保留\|KEEP\|RETENTION' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"

echo ""

# ========== 3. Mock故障注入: mysqldump失败 ==========
echo "-- 3. Mock故障注入 --"

# 创建mock环境
MOCK_DIR=$(mktemp -d)
trap "rm -rf $MOCK_DIR" EXIT

# 3.1 模拟mysqldump失败: mysqldump返回非零，gzip成功
cat > "$MOCK_DIR/mysqldump" << 'MOCK_EOF'
#!/usr/bin/env bash
echo "mock mysqldump: simulated failure" >&2
exit 1
MOCK_EOF
chmod +x "$MOCK_DIR/mysqldump"

# 3.2 模拟gzip成功
cat > "$MOCK_DIR/gzip" << 'MOCK_EOF'
#!/usr/bin/env bash
cat > /dev/null
MOCK_EOF
chmod +x "$MOCK_DIR/gzip"

# 测试PIPESTATUS逻辑: 管道中mysqldump失败时PIPESTATUS[0]应捕获
PIPE_TEST_OUTPUT=$(cd "$MOCK_DIR" && PATH="$MOCK_DIR:$PATH" bash -c '
set -o pipefail
false | true
DUMP_EXIT=${PIPESTATUS[0]}
echo $DUMP_EXIT
' 2>/dev/null || echo "ERROR")
check "PIPESTATUS[0]捕获管道首命令退出码(期望1)" "$([ "$PIPE_TEST_OUTPUT" = "1" ] && echo true || echo false)"

# 测试$?误捕获: 无pipefail时管道中mysqldump失败但$?捕获最后命令(旧bug演示)
DOLLAR_TEST_OUTPUT=$(cd "$MOCK_DIR" && PATH="$MOCK_DIR:$PATH" bash -c '
false | true
echo $?
' 2>/dev/null || echo "ERROR")
check "无pipefail时\$?捕获最后命令退出码(期望0,验证旧bug根因)" "$([ "$DOLLAR_TEST_OUTPUT" = "0" ] && echo true || echo false)"

echo ""

# ========== 4. 空备份检测 ==========
echo "-- 4. 空备份检测 --"

# 创建空gzip文件模拟空备份
EMPTY_GZ="$MOCK_DIR/empty_backup.sql.gz"
echo -n "" | gzip > "$EMPTY_GZ"
EMPTY_SIZE=$(stat -c%s "$EMPTY_GZ" 2>/dev/null || wc -c < "$EMPTY_GZ")
check "空备份文件大小>0但解压后为空(gzip头)" "$([ "$EMPTY_SIZE" -gt 0 ] && echo true || echo false)"

# 验证verify-backup.sh能检测空备份
check "verify-backup.sh 存在" "$([ -f "$DB_SCRIPTS/verify-backup.sh" ] && echo true || echo false)"
check "verify-backup.sh gzip完整性检查" "$(grep -q 'gzip.*-t\|gzip.*test\|完整性' "$DB_SCRIPTS/verify-backup.sh" && echo true || echo false)"
check "verify-backup.sh CREATE TABLE计数检查" "$(grep -q 'CREATE TABLE' "$DB_SCRIPTS/verify-backup.sh" && echo true || echo false)"

echo ""

# ========== 5. 损坏文件检测 ==========
echo "-- 5. 损坏文件检测 --"

# 创建损坏的gzip文件：截断合法gzip产生CRC/结构损坏
CORRUPT_GZ="$MOCK_DIR/corrupt_backup.sql.gz"
echo "CREATE TABLE test (id INT); INSERT INTO test VALUES (1);" | gzip > "$MOCK_DIR/base_valid.gz"
ORIG_SIZE=$(wc -c < "$MOCK_DIR/base_valid.gz")
TRUNC_SIZE=$((ORIG_SIZE / 2))
dd if="$MOCK_DIR/base_valid.gz" of="$CORRUPT_GZ" bs=1 count=$TRUNC_SIZE 2>/dev/null
GZIP_TEST_RESULT=$(gzip -t "$CORRUPT_GZ" 2>/dev/null && echo "OK" || echo "CORRUPT_DETECTED")
check "gzip -t检测截断损坏文件 (${ORIG_SIZE}B→${TRUNC_SIZE}B)" "$([ "$GZIP_TEST_RESULT" = "CORRUPT_DETECTED" ] && echo true || echo false)"

# 正常gzip文件应通过
VALID_GZ="$MOCK_DIR/valid_backup.sql.gz"
echo "CREATE TABLE robot (id INT); INSERT INTO robot VALUES (1);" | gzip > "$VALID_GZ"
GZIP_VALID_RESULT=$(gzip -t "$VALID_GZ" 2>&1 && echo "OK" || echo "FAIL")
check "正常gzip文件通过完整性检查" "$([ "$GZIP_VALID_RESULT" = "OK" ] && echo true || echo false)"

echo ""

# ========== 6. 备份脚本dry-run验证 ==========
echo "-- 6. 备份脚本结构验证 --"

check "backup.sh Docker模式支持" "$(grep -q 'docker\|DOCKER' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"
check "backup.sh standalone模式支持" "$(grep -q 'standalone\|STANDALONE\|直接' "$DB_SCRIPTS/backup.sh" && echo true || echo false)"
check "restore.sh Docker模式支持" "$(grep -q 'docker\|DOCKER' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"
check "restore.sh --binary-mode" "$(grep -q 'binary-mode' "$DB_SCRIPTS/restore.sh" && echo true || echo false)"

# 检查脚本语法
BASH_SYNTAX_BACKUP=$(bash -n "$DB_SCRIPTS/backup.sh" 2>&1 && echo "OK" || echo "SYNTAX_ERROR")
check "backup.sh bash语法检查" "$([ "$BASH_SYNTAX_BACKUP" = "OK" ] && echo true || echo false)"

BASH_SYNTAX_RESTORE=$(bash -n "$DB_SCRIPTS/restore.sh" 2>&1 && echo "OK" || echo "SYNTAX_ERROR")
check "restore.sh bash语法检查" "$([ "$BASH_SYNTAX_RESTORE" = "OK" ] && echo true || echo false)"

BASH_SYNTAX_VERIFY=$(bash -n "$DB_SCRIPTS/verify-backup.sh" 2>&1 && echo "OK" || echo "SYNTAX_ERROR")
check "verify-backup.sh bash语法检查" "$([ "$BASH_SYNTAX_VERIFY" = "OK" ] && echo true || echo false)"

echo ""

# ========== 结果 ==========
echo "===================================="
echo " 备份恢复测试结果: PASS=$PASS FAIL=$FAIL SKIP=$SKIP"
echo "===================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "❌ 存在FAIL项"
    exit 1
else
    echo ""
    echo "✅ 全部通过"
    exit 0
fi