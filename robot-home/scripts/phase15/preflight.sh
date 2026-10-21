#!/usr/bin/env bash
# 机器人之家 Phase15 Night4 — 排练前置检查
# 验证Docker/Compose/端口/SQL文件/nginx.conf是否就绪
# 用法: bash scripts/phase15/preflight.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROBOT_HOME="$(cd "$SCRIPT_DIR/../.." && pwd)"
PHASE15_DIR="$ROBOT_HOME/deploy/phase15"
SQL_DIR="$ROBOT_HOME/sql"

PASS=0
FAIL=0

check() {
    local desc="$1"
    local result="$2"
    if [ "$result" = "true" ]; then
        echo "  [PASS] $desc"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $desc"
        FAIL=$((FAIL + 1))
    fi
}

echo "===================================="
echo " Phase15 Night4 Rehearsal Preflight"
echo "===================================="
echo ""

echo "-- 1. Docker环境 --"
if command -v docker &>/dev/null; then
    check "docker命令可用" "true"
    if docker info &>/dev/null 2>&1; then
        check "Docker daemon运行中" "true"
    else
        check "Docker daemon运行中" "false"
    fi
else
    check "docker命令可用" "false"
    check "Docker daemon运行中" "false"
fi

if docker compose version &>/dev/null 2>&1; then
    check "docker compose v2可用" "true"
elif command -v docker-compose &>/dev/null 2>&1; then
    check "docker-compose v1可用" "true"
else
    check "docker compose可用" "false"
fi
echo ""

echo "-- 2. 端口可用性 --"
REQUIRED_PORTS=(13306 13307 16379 19000 19001 18080)
for port in "${REQUIRED_PORTS[@]}"; do
    if command -v ss &>/dev/null; then
        if ss -tlnp 2>/dev/null | grep -q ":$port "; then
            check "端口 $port 可用" "false"
        else
            check "端口 $port 可用" "true"
        fi
    elif command -v netstat &>/dev/null; then
        if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
            check "端口 $port 可用" "false"
        else
            check "端口 $port 可用" "true"
        fi
    else
        echo "  [SKIP] 端口 $port (无ss/netstat命令)"
    fi
done
echo ""

echo "-- 3. SQL初始化链完整性 --"
REQUIRED_SQLS=(
    "01_schema.sql"
    "02_init_data.sql"
    "03_demo_data.sql"
    "04_crawler_schema.sql"
    "04_migration_source_url.sql"
    "05_brand_alias_and_indexes.sql"
    "06_real_brands_companies.sql"
    "07_real_robot_products.sql"
    "08_search_indexes.sql"
    "09_crawler_sources.sql"
    "10_phase6_beta.sql"
    "11_phase7_content_community_growth.sql"
    "12_phase8_community_procurement_growth.sql"
    "13_phase9_product_growth.sql"
    "14_phase10_ecosystem_growth.sql"
    "15_phase12_real_data_growth.sql"
    "16_phase12_data_quality_snapshot.sql"
    "17_phase13_growth_enterprise.sql"
    "18_phase14_commercialization.sql"
)
for sqlfile in "${REQUIRED_SQLS[@]}"; do
    if [ -f "$SQL_DIR/$sqlfile" ]; then
        check "$sqlfile 存在" "true"
    else
        check "$sqlfile 存在" "false"
    fi
done
echo ""

echo "-- 4. 排练配置文件 --"
if [ -f "$PHASE15_DIR/compose.rehearsal.yml" ]; then
    check "compose.rehearsal.yml 存在" "true"
else
    check "compose.rehearsal.yml 存在" "false"
fi

if [ -f "$PHASE15_DIR/.env.rehearsal" ]; then
    check ".env.rehearsal 存在" "true"
elif [ -f "$PHASE15_DIR/.env.rehearsal.example" ]; then
    check ".env.rehearsal.example 存在(需cp为.env.rehearsal)" "true"
    echo "  [WARN] 请执行: cp $PHASE15_DIR/.env.rehearsal.example $PHASE15_DIR/.env.rehearsal"
else
    check ".env.rehearsal 存在" "false"
fi

if [ -f "$ROBOT_HOME/deploy/nginx.conf" ]; then
    check "deploy/nginx.conf 存在" "true"
else
    check "deploy/nginx.conf 存在" "false"
fi
echo ""

echo "-- 5. Flyway迁移文件 --"
FLYWAY_DIR="$ROBOT_HOME/robot-home-server/src/main/resources/db/migration"
if [ -d "$FLYWAY_DIR" ]; then
    FLYWAY_COUNT=$(find "$FLYWAY_DIR" -name "V*.sql" | wc -l)
    if [ "$FLYWAY_COUNT" -gt 0 ]; then
        check "Flyway迁移文件存在(${FLYWAY_COUNT}个)" "true"
    else
        check "Flyway迁移文件存在" "false"
    fi
else
    check "Flyway迁移目录存在" "false"
fi
echo ""

echo "===================================="
echo " Preflight结果: PASS=$PASS FAIL=$FAIL"
echo "===================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "❌ 前置检查未通过，请修复上述FAIL项后再启动排练"
    exit 1
else
    echo ""
    echo "✅ 前置检查全部通过，可以启动排练:"
    echo "   bash scripts/phase15/run-rehearsal.sh"
    exit 0
fi