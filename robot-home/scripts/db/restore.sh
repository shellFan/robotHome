#!/usr/bin/env bash
# ============================================================
# 机器人之家 — MySQL 数据库恢复脚本
# 用法：
#   ./restore.sh <备份文件.sql.gz>              # Docker compose 模式
#   ./restore.sh <备份文件.sql.gz> --standalone # 直连 MySQL 模式
# ⚠️ 警告：恢复将覆盖目标数据库中的现有数据！
#   脚本会在恢复前要求二次确认，输入 YES 继续
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
CONTAINER_NAME="robot-home-mysql"
DATABASE="robot_home"
MODE="docker"

# 解析参数
BACKUP_FILE=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --standalone) MODE="standalone"; shift ;;
        *)            BACKUP_FILE="$1"; shift ;;
    esac
done

if [[ -z "${BACKUP_FILE}" ]]; then
    echo "用法: $0 <备份文件.sql.gz> [--standalone]"
    echo ""
    echo "可用备份:"
    ls -lht "${SCRIPT_DIR}/../../backups/"robot_home_*.sql.gz 2>/dev/null | head -10 || echo "  （无备份文件）"
    exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

# 安全确认
echo "========================================"
echo "⚠️  数据库恢复操作"
echo "========================================"
echo "备份文件: ${BACKUP_FILE}"
echo "目标数据库: ${DATABASE}"
echo "模式: ${MODE}"
echo "⚠️  此操作将覆盖目标数据库中的现有数据！"
echo ""
read -r -p "确认恢复？输入 YES 继续: " CONFIRM
if [[ "${CONFIRM}" != "YES" ]]; then
    echo "已取消恢复"
    exit 0
fi

echo ""
echo "=== 开始恢复 ==="

if [[ "${MODE}" == "docker" ]]; then
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "错误: MySQL 容器 ${CONTAINER_NAME} 未运行"
        exit 1
    fi

    # 恢复前自动备份当前状态（安全网）
    PRE_RESTORE_BACKUP="${BACKUP_DIR}/pre_restore_$(date +%Y%m%d_%H%M%S).sql.gz"
    echo "创建恢复前备份: ${PRE_RESTORE_BACKUP}"
    # PIPESTATUS[0] 精确捕获 mysqldump 退出码（gzip 成功不代表 mysqldump 成功）
    set +e
    docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-root}" "${CONTAINER_NAME}" \
        mysqldump \
        --host=127.0.0.1 \
        --user=root \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --connect-timeout=10 \
        "${DATABASE}" | gzip > "${PRE_RESTORE_BACKUP}"
    PRE_DUMP_EXIT=${PIPESTATUS[0]}
    set -e
    if [[ ${PRE_DUMP_EXIT} -ne 0 ]]; then
        echo "警告: 恢复前备份失败，继续恢复（当前状态将无法回退）"
        rm -f "${PRE_RESTORE_BACKUP}"
    fi

    # 先解压再通过 docker exec 导入
    # --binary-mode: 避免字符编码问题
    # --connect-timeout: 连接超时
    echo "导入备份文件..."
    if ! gunzip -c "${BACKUP_FILE}" | docker exec -i -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-root}" "${CONTAINER_NAME}" \
        mysql \
        --host=127.0.0.1 \
        --user=root \
        --binary-mode \
        --connect-timeout=10 \
        "${DATABASE}"; then
        echo "错误: 恢复失败！可使用恢复前备份回退: ${PRE_RESTORE_BACKUP}"
        exit 1
    fi
else
    : "${MYSQL_HOST:?MYSQL_HOST 必须设置}"
    : "${MYSQL_USER:?MYSQL_USER 必须设置}"
    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD 必须设置}"
    DATABASE="${MYSQL_DATABASE:-robot_home}"

    # 恢复前自动备份
    PRE_RESTORE_BACKUP="${BACKUP_DIR}/pre_restore_$(date +%Y%m%d_%H%M%S).sql.gz"
    echo "创建恢复前备份: ${PRE_RESTORE_BACKUP}"
    export MYSQL_PWD="${MYSQL_PASSWORD}"
    # PIPESTATUS[0] 精确捕获 mysqldump 退出码
    set +e
    mysqldump \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT:-3306}" \
        --user="${MYSQL_USER}" \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --connect-timeout=10 \
        "${DATABASE}" | gzip > "${PRE_RESTORE_BACKUP}"
    PRE_DUMP_EXIT=${PIPESTATUS[0]}
    set -e
    if [[ ${PRE_DUMP_EXIT} -ne 0 ]]; then
        echo "警告: 恢复前备份失败，继续恢复（当前状态将无法回退）"
        rm -f "${PRE_RESTORE_BACKUP}"
    fi

    echo "导入备份文件..."
    if ! gunzip -c "${BACKUP_FILE}" | mysql \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT:-3306}" \
        --user="${MYSQL_USER}" \
        --binary-mode \
        --connect-timeout=10 \
        "${DATABASE}"; then
        echo "错误: 恢复失败！可使用恢复前备份回退: ${PRE_RESTORE_BACKUP}"
        unset MYSQL_PWD
        exit 1
    fi
    unset MYSQL_PWD
fi

echo "=== 恢复完成 ==="
echo ""
echo "建议执行验证: ./verify-backup.sh"