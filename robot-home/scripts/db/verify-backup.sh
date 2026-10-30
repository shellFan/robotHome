#!/usr/bin/env bash
# ============================================================
# 机器人之家 — 备份完整性验证脚本
# 用法：
#   ./verify-backup.sh                       # 验证最新备份
#   ./verify-backup.sh <备份文件.sql.gz>     # 验证指定备份
# 验证项：
#   1. gzip 完整性（gunzip -t）
#   2. 如果存在 sidecar Manifest，校验格式与备份 SHA-256
#   3. SQL 文件非空且包含关键 DDL 语句
#   4. 关键表 CREATE TABLE 语句存在
#   5. INSERT 行数统计
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
BACKUP_FILE=""
ALLOW_EMPTY_DATA=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --allow-empty-data) ALLOW_EMPTY_DATA=true; shift ;;
        --*) echo "错误: 未知参数: $1"; exit 2 ;;
        *)
            if [[ -n "${BACKUP_FILE}" ]]; then
                echo "错误: 只能指定一个备份文件"
                exit 2
            fi
            BACKUP_FILE="$1"
            shift
            ;;
    esac
done

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
echo -n "[1/5] gzip 完整性... "
if gunzip -t "${BACKUP_FILE}" 2>/dev/null; then
    echo "PASS"
    PASS=$((PASS + 1))
else
    echo "FAIL（文件损坏）"
    FAIL=$((FAIL + 1))
fi

# 2. 校验新格式备份的 SHA-256 Manifest；兼容没有 sidecar 的历史备份
echo -n "[2/5] SHA-256 Manifest... "
MANIFEST_FILE="${BACKUP_FILE}.manifest"
BACKUP_BASENAME="${BACKUP_FILE##*/}"
NEW_FORMAT_BACKUP=false
if [[ "${BACKUP_BASENAME}" =~ ^(robot_home|pre_restore)_[0-9]{8}_[0-9]{6}_[0-9]+_[0-9]+\.sql\.gz$ ]]; then
    NEW_FORMAT_BACKUP=true
fi
if [[ -f "${MANIFEST_FILE}" ]]; then
    if MANIFEST_SHA=$(awk -F= '
    {
        if (NF != 2) { invalid = 1; next }
        key = $1
        value = $2
        if (key !~ /^(format|database|sha256|created_at|mode)$/ || key in seen) {
            invalid = 1
            next
        }
        seen[key] = 1
        field_count++
        fields[key] = value
    }
    END {
        if (invalid || field_count != 5 || fields["format"] != "robot-home-backup-v1" ||
            fields["database"] == "" || fields["created_at"] == "" ||
            (fields["mode"] != "docker" && fields["mode"] != "standalone") ||
            length(fields["sha256"]) != 64 || fields["sha256"] !~ /^[a-fA-F0-9]+$/) exit 1
        print fields["sha256"]
    }' "${MANIFEST_FILE}"); then
        if command -v sha256sum >/dev/null 2>&1; then
            ACTUAL_SHA=$(sha256sum -- "${BACKUP_FILE}" | awk '{ print $1 }')
        elif command -v shasum >/dev/null 2>&1; then
            ACTUAL_SHA=$(shasum -a 256 "${BACKUP_FILE}" | awk '{ print $1 }')
        else
            ACTUAL_SHA=""
        fi
        if [[ -z "${ACTUAL_SHA}" ]]; then
            echo "FAIL（无法计算备份 SHA-256）"
            FAIL=$((FAIL + 1))
        elif [[ "$(printf '%s' "${MANIFEST_SHA}" | tr '[:upper:]' '[:lower:]')" != "$(printf '%s' "${ACTUAL_SHA}" | tr '[:upper:]' '[:lower:]')" ]]; then
            echo "FAIL（Manifest SHA-256 不匹配）"
            FAIL=$((FAIL + 1))
        else
            echo "PASS"
            PASS=$((PASS + 1))
        fi
    else
        echo "FAIL（Manifest 格式无效）"
        FAIL=$((FAIL + 1))
    fi
elif [[ "${NEW_FORMAT_BACKUP}" == "true" ]]; then
    echo "FAIL（新格式备份缺少 SHA-256 Manifest）"
    FAIL=$((FAIL + 1))
else
    echo "SKIP（无 sidecar，兼容旧备份）"
fi

# 3. SQL 非空且包含 DDL
echo -n "[3/5] SQL 内容检查... "
if DDL_COUNT=$(gunzip -c "${BACKUP_FILE}" | awk '/CREATE TABLE/ { count++ } END { print count + 0 }'); then
    if [[ "${DDL_COUNT}" -gt 10 ]]; then
        echo "PASS（${DDL_COUNT} 个 CREATE TABLE）"
        PASS=$((PASS + 1))
    else
        echo "FAIL（仅 ${DDL_COUNT} 个 CREATE TABLE，预期 >10）"
        FAIL=$((FAIL + 1))
    fi
else
    echo "FAIL（SQL 内容无法完整读取）"
    FAIL=$((FAIL + 1))
fi

# 4. 关键表存在性
echo -n "[4/5] 关键表检查... "
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

# 5. INSERT 行数统计
echo -n "[5/5] 数据行数统计... "
if INSERT_COUNT=$(gunzip -c "${BACKUP_FILE}" | awk '/^INSERT INTO/ { count++ } END { print count + 0 }'); then
    if [[ "${INSERT_COUNT}" -gt 0 ]]; then
        echo "PASS（${INSERT_COUNT} 条 INSERT 语句）"
        PASS=$((PASS + 1))
    elif [[ "${ALLOW_EMPTY_DATA}" == "true" ]]; then
        echo "PASS（显式允许空数据）"
        PASS=$((PASS + 1))
    else
        echo "FAIL（0 条 INSERT；空数据需显式使用 --allow-empty-data）"
        FAIL=$((FAIL + 1))
    fi
else
    echo "FAIL（无法完整读取 INSERT 数据）"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "=== 验证结果: ${PASS} PASS / ${FAIL} FAIL ==="
if [[ "${FAIL}" -gt 0 ]]; then
    exit 1
fi
