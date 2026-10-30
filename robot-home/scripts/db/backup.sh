#!/usr/bin/env bash
# ============================================================
# 机器人之家 — MySQL 数据库备份脚本
# 用法：bash backup.sh [--standalone] [--retain N] [--allow-empty-data]
# Docker 模式必须设置 MYSQL_ROOT_PASSWORD；standalone 模式必须设置：
# MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASSWORD / MYSQL_DATABASE
# 产出：robot_home_<timestamp>_<pid>_<random>.sql.gz 和 SHA-256 Manifest
# ============================================================
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
CONTAINER_NAME="robot-home-mysql"
DATABASE="robot_home"
RETAIN=30
MODE="docker"
ALLOW_EMPTY_DATA=false
LOCK_FILE="${BACKUP_DIR}/.database-operation.lock"
LOCK_ACQUIRED=false
TEMP_BACKUP_FILE=""
TEMP_MANIFEST_FILE=""
BACKUP_FILE=""
BACKUP_MANIFEST=""
FINAL_CREATED=false

cleanup() {
    if [[ -n "${TEMP_BACKUP_FILE}" ]]; then rm -f -- "${TEMP_BACKUP_FILE}" || true; fi
    if [[ -n "${TEMP_MANIFEST_FILE}" ]]; then rm -f -- "${TEMP_MANIFEST_FILE}" || true; fi
    if [[ "${FINAL_CREATED}" == "true" ]]; then rm -f -- "${BACKUP_FILE}" "${BACKUP_MANIFEST}" || true; fi
    if [[ "${LOCK_ACQUIRED}" == "true" ]]; then
        flock -u 9 2>/dev/null || true
        exec 9>&-
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

while [[ $# -gt 0 ]]; do
    case "$1" in
        --standalone) MODE="standalone"; shift ;;
        --allow-empty-data) ALLOW_EMPTY_DATA=true; shift ;;
        --retain)
            if [[ $# -lt 2 ]]; then echo "错误: --retain 缺少数量"; exit 2; fi
            RETAIN="$2"
            shift 2
            ;;
        *) echo "未知参数: $1"; exit 2 ;;
    esac
done

if [[ ! "${RETAIN}" =~ ^[0-9]+$ ]]; then
    echo "错误: --retain must be a positive integer"
    exit 2
fi
RETAIN="${RETAIN#"${RETAIN%%[!0]*}"}"
if [[ -z "${RETAIN}" ]]; then
    echo "错误: --retain must be a positive integer"
    exit 2
fi

if [[ "${MODE}" == "docker" ]]; then
    : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set for Docker mode}"
else
    : "${MYSQL_HOST:?MYSQL_HOST 必须设置}"
    : "${MYSQL_USER:?MYSQL_USER 必须设置}"
    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD 必须设置}"
    DATABASE="${MYSQL_DATABASE:-robot_home}"
fi

mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"
if ! command -v flock >/dev/null 2>&1; then
    echo "错误: 找不到 flock 工具，无法安全锁定数据库操作"
    exit 1
fi
if ! exec 9>>"${LOCK_FILE}"; then
    echo "错误: 无法打开数据库操作锁文件: ${LOCK_FILE}"
    exit 1
fi
if ! flock -n 9; then
    exec 9>&-
    echo "错误: 另一个备份或恢复操作正在运行（锁文件: ${LOCK_FILE}）"
    exit 1
fi
LOCK_ACQUIRED=true
chmod 600 "${LOCK_FILE}"

if ! DF_OUTPUT=$(df -Pm "${BACKUP_DIR}" 2>/dev/null); then
    echo "错误: 无法检查备份目录磁盘空间，停止备份"
    exit 1
fi
AVAILABLE_MB=$(echo "${DF_OUTPUT}" | awk 'NR == 2 && $4 ~ /^[0-9]+$/ { print $4 }')
if [[ ! "${AVAILABLE_MB}" =~ ^[0-9]+$ ]]; then
    echo "错误: 无法解析备份目录可用磁盘空间，停止备份"
    exit 1
fi
if [[ "${AVAILABLE_MB}" -lt 500 ]]; then
    echo "错误: 磁盘空间不足（可用 ${AVAILABLE_MB}MB，需要至少 500MB）"
    exit 1
fi

if [[ "${MODE}" == "docker" ]] && ! docker ps --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
    echo "错误: MySQL 容器 ${CONTAINER_NAME} 未运行"
    echo "提示: 使用 --standalone 模式直连远程数据库"
    exit 1
fi

TIMESTAMP=$(date -u +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/robot_home_${TIMESTAMP}_$$_${RANDOM}.sql.gz"
BACKUP_MANIFEST="${BACKUP_FILE}.manifest"
if [[ -e "${BACKUP_FILE}" || -e "${BACKUP_MANIFEST}" ]]; then
    echo "错误: 备份目标文件已存在，停止以避免覆盖: ${BACKUP_FILE}"
    exit 1
fi
TEMP_BACKUP_FILE=$(mktemp "${BACKUP_DIR}/.robot_home_backup.XXXXXX")

echo "=== 机器人之家数据库备份 ==="
echo "模式: ${MODE}"
echo "数据库: ${DATABASE}"
echo "目标文件: ${BACKUP_FILE}"
echo "开始 mysqldump..."

if [[ "${MODE}" == "docker" ]]; then
    set +e
    MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" docker exec -e MYSQL_PWD "${CONTAINER_NAME}" mysqldump --host=127.0.0.1 --user=root --single-transaction --routines --triggers --events --quick --lock-tables=false --connect-timeout=10 "${DATABASE}" | gzip > "${TEMP_BACKUP_FILE}"
    PIPE_STATUS=("${PIPESTATUS[@]}")
    DUMP_EXIT=${PIPE_STATUS[0]:-1}
    GZIP_EXIT=${PIPE_STATUS[1]:-1}
    set -e
else
    export MYSQL_PWD="${MYSQL_PASSWORD}"
    set +e
    mysqldump --host="${MYSQL_HOST}" --port="${MYSQL_PORT:-3306}" --user="${MYSQL_USER}" --single-transaction --routines --triggers --events --quick --lock-tables=false --connect-timeout=10 "${DATABASE}" | gzip > "${TEMP_BACKUP_FILE}"
    PIPE_STATUS=("${PIPESTATUS[@]}")
    DUMP_EXIT=${PIPE_STATUS[0]:-1}
    GZIP_EXIT=${PIPE_STATUS[1]:-1}
    set -e
    unset MYSQL_PWD
fi

if [[ ${DUMP_EXIT} -ne 0 || ${GZIP_EXIT} -ne 0 ]]; then
    echo "错误: 备份管道失败（mysqldump=${DUMP_EXIT}, gzip=${GZIP_EXIT}），拒绝发布不完整备份"
    exit 1
fi
if ! gzip -t "${TEMP_BACKUP_FILE}"; then
    echo "错误: 生成的 gzip 文件校验失败"
    exit 1
fi
FILE_SIZE=$(stat -c%s "${TEMP_BACKUP_FILE}" 2>/dev/null || stat -f%z "${TEMP_BACKUP_FILE}")
if [[ "${FILE_SIZE}" -lt 1024 ]]; then
    echo "错误: 备份文件过小 (${FILE_SIZE} bytes)，可能备份失败"
    exit 1
fi

VERIFY_ARGS=("${TEMP_BACKUP_FILE}")
if [[ "${ALLOW_EMPTY_DATA}" == "true" ]]; then VERIFY_ARGS+=(--allow-empty-data); fi
if ! bash "${SCRIPT_DIR}/verify-backup.sh" "${VERIFY_ARGS[@]}"; then
    echo "错误: SQL 结构/数据校验失败，拒绝发布备份"
    exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
    ARCHIVE_SHA=$(sha256sum "${TEMP_BACKUP_FILE}" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
    ARCHIVE_SHA=$(shasum -a 256 "${TEMP_BACKUP_FILE}" | awk '{print $1}')
else
    echo "错误: 找不到 SHA-256 工具，拒绝发布备份"
    exit 1
fi
if [[ ! "${ARCHIVE_SHA}" =~ ^[a-fA-F0-9]{64}$ ]]; then
    echo "错误: 无法生成有效 SHA-256，拒绝发布备份"
    exit 1
fi
ARCHIVE_SHA_LOWER=$(printf '%s' "${ARCHIVE_SHA}" | tr '[:upper:]' '[:lower:]')

TEMP_MANIFEST_FILE=$(mktemp "${BACKUP_DIR}/.robot_home_manifest.XXXXXX")
cat > "${TEMP_MANIFEST_FILE}" <<MANIFEST
format=robot-home-backup-v1
database=${DATABASE}
sha256=${ARCHIVE_SHA_LOWER}
created_at=${TIMESTAMP}
mode=${MODE}
MANIFEST
chmod 600 "${TEMP_BACKUP_FILE}" "${TEMP_MANIFEST_FILE}"

FINAL_CREATED=true
mv -- "${TEMP_MANIFEST_FILE}" "${BACKUP_MANIFEST}"
TEMP_MANIFEST_FILE=""
mv -- "${TEMP_BACKUP_FILE}" "${BACKUP_FILE}"
TEMP_BACKUP_FILE=""
FINAL_CREATED=false

echo "备份完成: ${BACKUP_FILE} ($(numfmt --to=iec "${FILE_SIZE}" 2>/dev/null || echo "${FILE_SIZE} bytes"))"
echo "校验清单: ${BACKUP_MANIFEST}"

cd "${BACKUP_DIR}"
BACKUP_FILES=()
while IFS= read -r backup_file; do
    [[ -n "${backup_file}" ]] && BACKUP_FILES+=("${backup_file}")
done < <(ls -1t robot_home_*.sql.gz 2>/dev/null || true)

BACKUP_COUNT=${#BACKUP_FILES[@]}
COUNT_TEXT="${BACKUP_COUNT}"
if [[ ${#RETAIN} -lt ${#COUNT_TEXT} ]] || \
    { [[ ${#RETAIN} -eq ${#COUNT_TEXT} ]] && [[ "${RETAIN}" < "${COUNT_TEXT}" ]]; }; then
    RETAIN_COUNT=$((10#${RETAIN}))
else
    RETAIN_COUNT=${BACKUP_COUNT}
fi

for ((index = RETAIN_COUNT; index < BACKUP_COUNT; index++)); do
    old_file="${BACKUP_FILES[index]}"
    echo "清理旧备份: ${old_file}"
    rm -f -- "${old_file}" "${old_file}.manifest"
done

BACKUP_COUNT=${RETAIN_COUNT}
echo "当前备份数: ${BACKUP_COUNT} / ${RETAIN}"
