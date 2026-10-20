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
    # 先解压再通过 docker exec 导入
    # --binary-mode: 避免字符编码问题
    gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" \
        mysql \
        --host=127.0.0.1 \
        --user=root \
        --password="${MYSQL_ROOT_PASSWORD:-root}" \
        --binary-mode \
        "${DATABASE}"
else
    : "${MYSQL_HOST:?MYSQL_HOST 必须设置}"
    : "${MYSQL_USER:?MYSQL_USER 必须设置}"
    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD 必须设置}"
    DATABASE="${MYSQL_DATABASE:-robot_home}"
    gunzip -c "${BACKUP_FILE}" | mysql \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT:-3306}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASSWORD}" \
        --binary-mode \
        "${DATABASE}"
fi

echo "=== 恢复完成 ==="
echo ""
echo "建议执行验证: ./verify-backup.sh"