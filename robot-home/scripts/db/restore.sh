#!/usr/bin/env bash
# ============================================================
# 机器人之家 — MySQL 数据库恢复脚本
# 用法：
#   ./restore.sh <备份文件.sql.gz>              # Docker compose 模式
#   ./restore.sh <备份文件.sql.gz> --standalone # 直连 MySQL 模式
#   ./restore.sh <备份文件.sql.gz> --allow-empty-data # 允许结构空库备份
# ⚠️ 警告：恢复将覆盖目标数据库中的现有数据！
#   脚本会在恢复前要求二次确认，输入 YES 继续
# ============================================================
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../../backups"
CONTAINER_NAME="robot-home-mysql"
DATABASE="robot_home"
MODE="docker"
LOCK_FILE="${BACKUP_DIR}/.database-operation.lock"
LOCK_ACQUIRED=false
RESTORE_INPUT_DIR=""
RESTORE_INPUT_FILE=""
PRE_RESTORE_BACKUP=""
PRE_RESTORE_MANIFEST=""
PRE_RESTORE_TEMP=""
PRE_RESTORE_MANIFEST_TEMP=""
PRE_RESTORE_FINAL_CREATED=false

cleanup() {
    if [[ -n "${PRE_RESTORE_TEMP}" ]]; then rm -f -- "${PRE_RESTORE_TEMP}" || true; fi
    if [[ -n "${PRE_RESTORE_MANIFEST_TEMP}" ]]; then rm -f -- "${PRE_RESTORE_MANIFEST_TEMP}" || true; fi
    if [[ -n "${RESTORE_INPUT_DIR}" ]]; then rm -rf -- "${RESTORE_INPUT_DIR}" || true; fi
    if [[ "${PRE_RESTORE_FINAL_CREATED}" == "true" ]]; then
        rm -f -- "${PRE_RESTORE_BACKUP}" "${PRE_RESTORE_MANIFEST}" || true
    fi
    if [[ "${LOCK_ACQUIRED}" == "true" ]]; then
        flock -u 9 2>/dev/null || true
        exec 9>&-
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

finalize_pre_restore_snapshot() {
    local file_size archive_sha

    if ! file_size=$(stat -c%s "${PRE_RESTORE_TEMP}" 2>/dev/null || stat -f%z "${PRE_RESTORE_TEMP}"); then
        echo "错误: 无法读取恢复前备份文件大小"
        return 1
    fi
    if [[ "${file_size}" -lt 1024 ]] || ! gzip -t "${PRE_RESTORE_TEMP}"; then
        echo "错误: 恢复前备份不完整，停止恢复操作"
        return 1
    fi
    if ! bash "${SCRIPT_DIR}/verify-backup.sh" --allow-empty-data "${PRE_RESTORE_TEMP}"; then
        echo "错误: 恢复前备份 SQL 验证失败，停止恢复操作"
        return 1
    fi

    if command -v sha256sum >/dev/null 2>&1; then
        archive_sha=$(sha256sum -- "${PRE_RESTORE_TEMP}" | awk '{ print $1 }')
    elif command -v shasum >/dev/null 2>&1; then
        archive_sha=$(shasum -a 256 "${PRE_RESTORE_TEMP}" | awk '{ print $1 }')
    else
        echo "错误: 找不到 SHA-256 工具，停止恢复操作"
        return 1
    fi
    if [[ ! "${archive_sha}" =~ ^[a-fA-F0-9]{64}$ ]]; then
        echo "错误: 无法生成有效 SHA-256，停止恢复操作"
        return 1
    fi
    archive_sha_lower=$(printf '%s' "${archive_sha}" | tr '[:upper:]' '[:lower:]')

    if ! PRE_RESTORE_MANIFEST_TEMP=$(mktemp "${BACKUP_DIR}/.pre_restore_manifest.XXXXXX"); then
        echo "错误: 无法创建恢复前备份 Manifest 临时文件"
        return 1
    fi
    if ! cat > "${PRE_RESTORE_MANIFEST_TEMP}" <<MANIFEST
format=robot-home-backup-v1
database=${DATABASE}
sha256=${archive_sha_lower}
created_at=${PRE_RESTORE_TIMESTAMP}
mode=${MODE}
MANIFEST
    then
        echo "错误: 无法写入恢复前备份 Manifest"
        return 1
    fi
    if ! chmod 600 "${PRE_RESTORE_TEMP}" "${PRE_RESTORE_MANIFEST_TEMP}"; then
        echo "错误: 无法限制恢复前备份文件权限"
        return 1
    fi

    PRE_RESTORE_FINAL_CREATED=true
    if ! mv -- "${PRE_RESTORE_MANIFEST_TEMP}" "${PRE_RESTORE_MANIFEST}"; then
        echo "错误: 无法发布恢复前备份 Manifest"
        return 1
    fi
    PRE_RESTORE_MANIFEST_TEMP=""
    if ! mv -- "${PRE_RESTORE_TEMP}" "${PRE_RESTORE_BACKUP}"; then
        echo "错误: 无法发布恢复前备份归档"
        return 1
    fi
    PRE_RESTORE_TEMP=""
    PRE_RESTORE_FINAL_CREATED=false
    echo "恢复前备份已验证并发布: ${PRE_RESTORE_BACKUP}"
}

# 解析参数
BACKUP_FILE=""
ALLOW_EMPTY_DATA=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        --standalone) MODE="standalone"; shift ;;
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

if [[ "${MODE}" == "docker" ]]; then
    : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set for Docker mode}"
else
    : "${MYSQL_HOST:?MYSQL_HOST 必须设置}"
    : "${MYSQL_USER:?MYSQL_USER 必须设置}"
    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD 必须设置}"
    DATABASE="${MYSQL_DATABASE:-robot_home}"
fi

VERIFY_ARGS=("${BACKUP_FILE}")
if [[ "${ALLOW_EMPTY_DATA}" == "true" ]]; then VERIFY_ARGS+=(--allow-empty-data); fi
if ! bash "${SCRIPT_DIR}/verify-backup.sh" "${VERIFY_ARGS[@]}"; then
    echo "错误: 待恢复备份验证失败，拒绝恢复"
    exit 1
fi
mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"

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

# Verify and import the same private copy so the input cannot change mid-restore.
if ! RESTORE_INPUT_DIR=$(mktemp -d "${BACKUP_DIR}/.restore_input.XXXXXX"); then
    echo "错误: 无法创建恢复输入暂存目录"
    exit 1
fi
chmod 700 "${RESTORE_INPUT_DIR}"
RESTORE_INPUT_FILE="${RESTORE_INPUT_DIR}/${BACKUP_FILE##*/}"
if ! cp -- "${BACKUP_FILE}" "${RESTORE_INPUT_FILE}"; then
    echo "错误: 无法暂存待恢复备份"
    exit 1
fi
if [[ -f "${BACKUP_FILE}.manifest" ]] && ! cp -- "${BACKUP_FILE}.manifest" "${RESTORE_INPUT_FILE}.manifest"; then
    echo "错误: 无法暂存待恢复备份 Manifest"
    exit 1
fi
VERIFY_ARGS=("${RESTORE_INPUT_FILE}")
if [[ "${ALLOW_EMPTY_DATA}" == "true" ]]; then VERIFY_ARGS+=(--allow-empty-data); fi
if ! bash "${SCRIPT_DIR}/verify-backup.sh" "${VERIFY_ARGS[@]}"; then
    echo "错误: 确认后的备份副本验证失败，拒绝恢复"
    exit 1
fi

echo ""
echo "=== 开始恢复 ==="

if [[ "${MODE}" == "docker" ]]; then
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "错误: MySQL 容器 ${CONTAINER_NAME} 未运行"
        exit 1
    fi

    # 恢复前自动备份当前状态（安全网）
    PRE_RESTORE_TIMESTAMP=$(date -u +%Y%m%d_%H%M%S)
    PRE_RESTORE_BACKUP="${BACKUP_DIR}/pre_restore_${PRE_RESTORE_TIMESTAMP}_$$_${RANDOM}.sql.gz"
    PRE_RESTORE_MANIFEST="${PRE_RESTORE_BACKUP}.manifest"
    if [[ -e "${PRE_RESTORE_BACKUP}" || -e "${PRE_RESTORE_BACKUP}.manifest" ]]; then
        echo "错误: 恢复前备份目标已存在，停止恢复操作: ${PRE_RESTORE_BACKUP}"
        exit 1
    fi
    PRE_RESTORE_TEMP=$(mktemp "${BACKUP_DIR}/.pre_restore_backup.XXXXXX")
    echo "创建恢复前备份: ${PRE_RESTORE_BACKUP}"
    # 分别检查 mysqldump 与 gzip 的退出码；两者任一失败都停止恢复。
    set +e
    MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" docker exec -e MYSQL_PWD "${CONTAINER_NAME}" \
        mysqldump \
        --host=127.0.0.1 \
        --user=root \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        --connect-timeout=10 \
        "${DATABASE}" | gzip > "${PRE_RESTORE_TEMP}"
    PRE_PIPE_STATUS=("${PIPESTATUS[@]}")
    PRE_DUMP_EXIT=${PRE_PIPE_STATUS[0]:-1}
    PRE_GZIP_EXIT=${PRE_PIPE_STATUS[1]:-1}
    set -e
    if [[ ${PRE_DUMP_EXIT} -ne 0 || ${PRE_GZIP_EXIT} -ne 0 ]]; then
        echo "错误: 恢复前备份失败（mysqldump=${PRE_DUMP_EXIT}, gzip=${PRE_GZIP_EXIT}），停止恢复操作"
        exit 1
    fi
    if ! finalize_pre_restore_snapshot; then exit 1; fi

    # 先解压再通过 docker exec 导入
    # --binary-mode: 避免字符编码问题
    # --connect-timeout: 连接超时
    echo "导入备份文件..."
    if ! gunzip -c "${RESTORE_INPUT_FILE}" | MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" docker exec -i -e MYSQL_PWD "${CONTAINER_NAME}" \
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
    # 恢复前自动备份
    PRE_RESTORE_TIMESTAMP=$(date -u +%Y%m%d_%H%M%S)
    PRE_RESTORE_BACKUP="${BACKUP_DIR}/pre_restore_${PRE_RESTORE_TIMESTAMP}_$$_${RANDOM}.sql.gz"
    PRE_RESTORE_MANIFEST="${PRE_RESTORE_BACKUP}.manifest"
    if [[ -e "${PRE_RESTORE_BACKUP}" || -e "${PRE_RESTORE_BACKUP}.manifest" ]]; then
        echo "错误: 恢复前备份目标已存在，停止恢复操作: ${PRE_RESTORE_BACKUP}"
        exit 1
    fi
    PRE_RESTORE_TEMP=$(mktemp "${BACKUP_DIR}/.pre_restore_backup.XXXXXX")
    echo "创建恢复前备份: ${PRE_RESTORE_BACKUP}"
    export MYSQL_PWD="${MYSQL_PASSWORD}"
    # 分别检查 mysqldump 与 gzip 的退出码；两者任一失败都停止恢复。
    set +e
    mysqldump \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT:-3306}" \
        --user="${MYSQL_USER}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        --connect-timeout=10 \
        "${DATABASE}" | gzip > "${PRE_RESTORE_TEMP}"
    PRE_PIPE_STATUS=("${PIPESTATUS[@]}")
    PRE_DUMP_EXIT=${PRE_PIPE_STATUS[0]:-1}
    PRE_GZIP_EXIT=${PRE_PIPE_STATUS[1]:-1}
    set -e
    if [[ ${PRE_DUMP_EXIT} -ne 0 || ${PRE_GZIP_EXIT} -ne 0 ]]; then
        echo "错误: 恢复前备份失败（mysqldump=${PRE_DUMP_EXIT}, gzip=${PRE_GZIP_EXIT}），停止恢复操作"
        exit 1
    fi
    if ! finalize_pre_restore_snapshot; then exit 1; fi

    echo "导入备份文件..."
    if ! gunzip -c "${RESTORE_INPUT_FILE}" | mysql \
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
