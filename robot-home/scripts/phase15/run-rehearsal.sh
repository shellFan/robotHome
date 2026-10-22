#!/usr/bin/env bash
# 机器人之家 Phase15 Night4 — 启动排练环境
# 用法: bash scripts/phase15/run-rehearsal.sh [--skip-preflight]
# 前提: preflight.sh通过

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROBOT_HOME="$(cd "$SCRIPT_DIR/../.." && pwd)"
PHASE15_DIR="$ROBOT_HOME/deploy/phase15"
COMPOSE_FILE="$PHASE15_DIR/compose.rehearsal.yml"
ENV_FILE="$PHASE15_DIR/.env.rehearsal"

SKIP_PREFLIGHT=false
if [ "${1:-}" = "--skip-preflight" ]; then
    SKIP_PREFLIGHT=true
fi

echo "===================================="
echo " Phase15 Night4 Rehearsal 启动"
echo "===================================="

# 1. 前置检查
if [ "$SKIP_PREFLIGHT" = false ]; then
    echo "[1/5] 执行前置检查..."
    bash "$SCRIPT_DIR/preflight.sh"
else
    echo "[1/5] 跳过前置检查(--skip-preflight)"
fi

# 2. 确保env文件存在
if [ ! -f "$ENV_FILE" ]; then
    echo "[2/5] .env.rehearsal不存在，从example创建..."
    cp "$PHASE15_DIR/.env.rehearsal.example" "$ENV_FILE"
else
    echo "[2/5] .env.rehearsal已存在"
fi

# 3. 确定compose命令
if docker compose version &>/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# 4. 启动排练环境
echo "[3/5] 拉取镜像..."
$COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull

echo "[4/5] 启动排练容器..."
$COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo "[5/5] 等待MySQL初始化完成(最长120秒)..."
WAIT_COUNT=0
MAX_WAIT=120
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker exec robot-home-rehearsal-mysql mysqladmin ping -uroot -prehearsal_root_pw &>/dev/null 2>&1; then
        echo "  MySQL(13306)就绪 (${WAIT_COUNT}s)"
        break
    fi
    sleep 5
    WAIT_COUNT=$((WAIT_COUNT + 5))
    echo "  等待中... ${WAIT_COUNT}s"
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    echo "❌ MySQL初始化超时(${MAX_WAIT}s)"
    echo "查看日志: docker logs robot-home-rehearsal-mysql"
    exit 1
fi

# 等待upgrade实例
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker exec robot-home-rehearsal-mysql-upgrade mysqladmin ping -uroot -prehearsal_upgrade_pw &>/dev/null 2>&1; then
        echo "  MySQL-Upgrade(13307)就绪 (${WAIT_COUNT}s)"
        break
    fi
    sleep 5
    WAIT_COUNT=$((WAIT_COUNT + 5))
done

echo ""
echo "===================================="
echo " 排练环境已启动"
echo "===================================="
echo " MySQL(Fresh):      localhost:13306"
echo " MySQL(Upgrade):    localhost:13307"
echo " Redis:             localhost:16379"
echo " MinIO API:         localhost:19000"
echo " MinIO Console:     localhost:19001"
echo " Nginx:             localhost:18080"
echo ""
echo "下一步:"
echo "  bash scripts/phase15/verify-runtime.sh     # 运行时验证"
echo "  bash scripts/phase15/cleanup-rehearsal.sh  # 清理排练环境"