#!/usr/bin/env bash
# ============================================================
# 机器人之家 — MySQL 数据库备份脚本
# 用法：
#   ./backup.sh                          # Docker compose 模式（默认）
#   ./backup.sh --standalone             # 直连 MySQL 模式
#   ./backup.sh --retain 7               # 保留最近 7 份备份（默认 30）
# 环境变量（standalone 模式必须）：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASSWORD / MYSQL_DATABASE
# 产出：./backups/robot_home_YYYYMMDD_HHMMSS.sql.gz
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
CONTAINER_NAME="robot-home-mysql"
DATABASE="robot_home"
RETAIN=30
MODE="docker"

# 解析参数
while [[ $# -gt 0 ]]; do
    case "$1" in
        --standalone) MODE="standalone"; shift ;;
        --retain)     RETAIN="$2"; shift 2 ;;
        *)            echo "未知参数: $1"; exit 1 ;;
    esac
done

mkdir -p "${BACKUP_DIR}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/robot_home_${TIMESTAMP}.sql.gz"

echo "=== 机器人之家数据库备份 ==="
echo "模式: ${MODE}"
echo "数据库: ${DATABASE}"
echo "目标文件: ${BACKUP_FILE}"

if [[ "${MODE}" == "docker" ]]; then
    # 检查容器是否运行
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "错误: MySQL 容器 ${CONTAINER_NAME} 未运行"
        echo "提示: 使用 --standalone 模式直连远程数据库"
        exit 1
    fi
    # MySQL 5.6 兼容：不使用 --set-gtid-purged（5.6 无 GTID）
    # --single-transaction: InnoDB 一致性快照，不锁表
    # --routines --triggers: 包含存储过程和触发器
    # --events: 包含事件调度
    docker exec "${CONTAINER_NAME}" \
        mysqldump \
        --host=127.0.0.1 \
        --user=root \
        --password="${MYSQL_ROOT_PASSWORD:-root}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        "${DATABASE}" | gzip > "${BACKUP_FILE}"
else
    # standalone 模式：直连 MySQL
    : "${MYSQL_HOST:?MYSQL_HOST 必须设置}"
    : "${MYSQL_USER:?MYSQL_USER 必须设置}"
    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD 必须设置}"
    DATABASE="${MYSQL_DATABASE:-robot_home}"
    mysqldump \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT:-3306}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASSWORD}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        "${DATABASE}" | gzip > "${BACKUP_FILE}"
fi

# 验证备份文件
FILE_SIZE=$(stat -c%s "${BACKUP_FILE}" 2>/dev/null || stat -f%z "${BACKUP_FILE}")
if [[ "${FILE_SIZE}" -lt 1024 ]]; then
    echo "错误: 备份文件过小 (${FILE_SIZE} bytes)，可能备份失败"
    rm -f "${BACKUP_FILE}"
    exit 1
fi

echo "备份完成: ${BACKUP_FILE} ($(numfmt --to=iec ${FILE_SIZE} 2>/dev/null || echo ${FILE_SIZE} bytes))"

# 备份轮转：保留最近 RETAIN 份
cd "${BACKUP_DIR}"
ls -t robot_home_*.sql.gz 2>/dev/null | tail -n +$((RETAIN + 1)) | while read -r old_file; do
    echo "清理旧备份: ${old_file}"
    rm -f "${old_file}"
done

echo "当前备份数: $(ls -1 robot_home_*.sql.gz 2>/dev/null | wc -l) / ${RETAIN}"