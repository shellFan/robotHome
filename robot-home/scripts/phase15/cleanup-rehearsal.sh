#!/usr/bin/env bash
# 机器人之家 Phase15 Night4 — 清理排练环境
# 停止并删除排练容器和卷（不含.env.rehearsal）
# 用法: bash scripts/phase15/cleanup-rehearsal.sh [--keep-volumes]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROBOT_HOME="$(cd "$SCRIPT_DIR/../.." && pwd)"
PHASE15_DIR="$ROBOT_HOME/deploy/phase15"
COMPOSE_FILE="$PHASE15_DIR/compose.rehearsal.yml"
ENV_FILE="$PHASE15_DIR/.env.rehearsal"

KEEP_VOLUMES=false
if [ "${1:-}" = "--keep-volumes" ]; then
    KEEP_VOLUMES=true
fi

if docker compose version &>/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

echo "===================================="
echo " Phase15 Night4 Rehearsal 清理"
echo "===================================="

if [ ! -f "$ENV_FILE" ]; then
    cp "$PHASE15_DIR/.env.rehearsal.example" "$ENV_FILE" 2>/dev/null || true
fi

echo "[1/3] 停止排练容器..."
$COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down

if [ "$KEEP_VOLUMES" = false ]; then
    echo "[2/3] 删除排练卷..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down -v
else
    echo "[2/3] 保留卷(--keep-volumes)"
fi

echo "[3/3] 检查残留容器..."
REHEARSAL_CONTAINERS=$(docker ps -a --format '{{.Names}}' | grep "robot-home-rehearsal-" || true)
if [ -n "$REHEARSAL_CONTAINERS" ]; then
    echo "  发现残留容器，强制删除..."
    echo "$REHEARSAL_CONTAINERS" | xargs -r docker rm -f
else
    echo "  无残留容器"
fi

echo ""
echo "===================================="
echo " 排练环境已清理"
echo "===================================="
if [ "$KEEP_VOLUMES" = false ]; then
    echo " 卷已删除，下次启动将全新初始化"
else
    echo " 卷已保留，下次启动将复用数据"
fi
echo ""
echo "注意: .env.rehearsal未删除(可复用)"
echo "如需彻底清理: rm $ENV_FILE"