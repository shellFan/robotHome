#!/usr/bin/env bash
# ============================================================
# 机器人之家 — 备份完整性验证脚本
# 用法：
#   ./verify-backup.sh                       # 验证最新备份
#   ./verify-backup.sh <备份文件.sql.gz>     # 验证指定备份
# 验证项：
#   1. gzip 完整性（gunzip -t）
#   2. SQL 文件非空且包含关键 DDL 语句
#   3. 关键表 CREATE TABLE 语句存在
#   4. INSERT 行数统计
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
BACKUP_FILE="${1:-}"

# 未指定文件时取最新备份
if [[ -z "${BACKUP_FILE}" ]]; then
    BACKUP_FILE=$(ls -t "${BACKUP_DIR}"/robot_home_*.sql.gz 2>/dev/null | head -1)
    if [[ -z "${BACKUP_FILE}" ]]; then
        echo "错误: 未找到备份文件"
        exit 1
    fi
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

echo "=== 备份验证: ${BACKUP_FILE} ==="
echo ""

PASS=0
FAIL=0

# 1. gzip 完整性
echo -n "[1/4] gzip 完整性... "
if gunzip -t "${BACKUP_FILE}" 2>/dev/null; then
    echo "PASS"
    PASS=$((PASS + 1))
else
    echo "FAIL（文件损坏）"
    FAIL=$((FAIL + 1))
fi

# 2. SQL 非空且包含 DDL
echo -n "[2/4] SQL 内容检查... "
DDL_COUNT=$(gunzip -c "${BACKUP_FILE}" | grep -c "CREATE TABLE" || true)
if [[ "${DDL_COUNT}" -gt 10 ]]; then
    echo "PASS（${DDL_COUNT} 个 CREATE TABLE）"
    PASS=$((PASS + 1))
else
    echo "FAIL（仅 ${DDL_COUNT} 个 CREATE TABLE，预期 >10）"
    FAIL=$((FAIL + 1))
fi

# 3. 关键表存在性
echo -n "[3/4] 关键表检查... "
if MISSING_TABLES=$(gunzip -c "${BACKUP_FILE}" | awk '
BEGIN {
    required[1] = "user"
    required[2] = "robot_product"
    required[3] = "article"
    required[4] = "crawler_source"
    required[5] = "enterprise_plan"
    required[6] = "enterprise_subscription"
}
/CREATE TABLE/ {
    for (i = 1; i <= 6; i++) {
        table = "`" required[i] "`"
        if (index($0, table) > 0) found[required[i]] = 1
    }
}
END {
    for (i = 1; i <= 6; i++) {
        if (!found[required[i]]) missing = missing " " required[i]
    }
    print missing
}'); then
    if [[ -z "${MISSING_TABLES}" ]]; then
        echo "PASS（6/6 关键表存在）"
        PASS=$((PASS + 1))
    else
        echo "FAIL（缺失表:${MISSING_TABLES}）"
        FAIL=$((FAIL + 1))
    fi
else
    echo "FAIL（SQL 内容无法完整读取）"
    FAIL=$((FAIL + 1))
fi

# 4. INSERT 行数统计
echo -n "[4/4] 数据行数统计... "
INSERT_COUNT=$(gunzip -c "${BACKUP_FILE}" | grep -c "^INSERT INTO" || true)
if [[ "${INSERT_COUNT}" -gt 0 ]]; then
    echo "PASS（${INSERT_COUNT} 条 INSERT 语句）"
    PASS=$((PASS + 1))
else
    echo "WARN（0 条 INSERT，可能是纯结构备份）"
    PASS=$((PASS + 1))
fi

echo ""
echo "=== 验证结果: ${PASS} PASS / ${FAIL} FAIL ==="
if [[ "${FAIL}" -gt 0 ]]; then
    exit 1
fi
