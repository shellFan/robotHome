#!/usr/bin/env bash
# 机器人之家 Phase15 Night4 — 运行时验证
# 验证排练环境中的MySQL/Redis/MinIO/Nginx运行时状态
# 用法: bash scripts/phase15/verify-runtime.sh

set -euo pipefail

PASS=0
FAIL=0
SKIP=0

check() {
    local desc="$1"
    local result="$2"
    if [ "$result" = "true" ]; then
        echo "  [PASS] $desc"
        PASS=$((PASS + 1))
    elif [ "$result" = "skip" ]; then
        echo "  [SKIP] $desc"
        SKIP=$((SKIP + 1))
    else
        echo "  [FAIL] $desc"
        FAIL=$((FAIL + 1))
    fi
}

echo "===================================="
echo " Phase15 Night4 Runtime Verification"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "===================================="
echo ""

# ========== 1. MySQL Fresh (13306) ==========
echo "-- 1. MySQL Fresh实例 (localhost:13306) --"
MYSQL_FRESH="docker exec robot-home-rehearsal-mysql mysql -uroot -prehearsal_root_pw robot_home_rehearsal -N -e"

if docker ps --format '{{.Names}}' | grep -q "robot-home-rehearsal-mysql$"; then
    check "容器运行中" "true"

    # 1.1 核心表验证(FlywayBaselineGuard必需表)
    CORE_TABLES="robot brand company article crawler_source"
    for tbl in $CORE_TABLES; do
        COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_rehearsal' AND table_name='$tbl'" 2>/dev/null || echo "0")
        if [ "$COUNT" -ge 1 ]; then
            check "核心表 $tbl 存在" "true"
        else
            check "核心表 $tbl 存在" "false"
        fi
    done

    # 1.2 Phase13表
    P13_TABLES="company_claim company_profile_change"
    for tbl in $P13_TABLES; do
        COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_rehearsal' AND table_name='$tbl'" 2>/dev/null || echo "0")
        if [ "$COUNT" -ge 1 ]; then
            check "Phase13表 $tbl 存在" "true"
        else
            check "Phase13表 $tbl 存在" "false"
        fi
    done

    # 1.3 Phase14表
    P14_TABLES="enterprise_plan enterprise_subscription enterprise_entitlement commercial_order advertising_slot advertising_campaign"
    for tbl in $P14_TABLES; do
        COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_rehearsal' AND table_name='$tbl'" 2>/dev/null || echo "0")
        if [ "$COUNT" -ge 1 ]; then
            check "Phase14表 $tbl 存在" "true"
        else
            check "Phase14表 $tbl 存在" "false"
        fi
    done

    # 1.4 采集系统14张表
    CRAWLER_TABLES="crawler_source crawler_task crawler_url crawler_page crawler_article crawler_product crawler_media crawler_error crawler_match_record crawler_rule crawler_log crawler_product_change crawler_brand_alias crawler_param_mapping"
    CRAWLER_OK=0
    for tbl in $CRAWLER_TABLES; do
        COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_rehearsal' AND table_name='$tbl'" 2>/dev/null || echo "0")
        if [ "$COUNT" -ge 1 ]; then
            CRAWLER_OK=$((CRAWLER_OK + 1))
        fi
    done
    if [ "$CRAWLER_OK" -eq 14 ]; then
        check "采集系统14张表全部存在(${CRAWLER_OK}/14)" "true"
    else
        check "采集系统14张表全部存在(${CRAWLER_OK}/14)" "false"
    fi

    # 1.5 MySQL版本确认
    VERSION=$($MYSQL_FRESH "SELECT VERSION()" 2>/dev/null || echo "unknown")
    check "MySQL版本: $VERSION" "$(echo "$VERSION" | grep -q '^5\.' && echo true || echo false)"

    # 1.6 字符集确认
    CHARSET=$($MYSQL_FRESH "SELECT default_character_set_name FROM information_schema.schemata WHERE schema_name='robot_home_rehearsal'" 2>/dev/null || echo "unknown")
    check "数据库字符集: $CHARSET" "$(echo "$CHARSET" | grep -q 'utf8mb4' && echo true || echo false)"

    # 1.7 初始数据行数
    ROBOT_COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM robot" 2>/dev/null || echo "0")
    check "robot表初始数据(${ROBOT_COUNT}行)" "$([ "$ROBOT_COUNT" -gt 0 ] && echo true || echo false)"

    SOURCE_COUNT=$($MYSQL_FRESH "SELECT COUNT(*) FROM crawler_source" 2>/dev/null || echo "0")
    check "crawler_source表初始数据(${SOURCE_COUNT}行)" "$([ "$SOURCE_COUNT" -gt 0 ] && echo true || echo false)"
else
    check "MySQL Fresh容器运行中" "false"
fi
echo ""

# ========== 2. MySQL Upgrade (13307) ==========
echo "-- 2. MySQL Upgrade实例 (localhost:13307) --"
MYSQL_UPGRADE="docker exec robot-home-rehearsal-mysql-upgrade mysql -uroot -prehearsal_upgrade_pw robot_home_upgrade -N -e"

if docker ps --format '{{.Names}}' | grep -q "robot-home-rehearsal-mysql-upgrade$"; then
    check "容器运行中" "true"

    # 2.1 验证Phase14历史库完整性(13张必需表)
    ALL_REQUIRED="robot brand company article crawler_source company_claim company_profile_change enterprise_plan enterprise_subscription enterprise_entitlement commercial_order advertising_slot advertising_campaign"
    REQUIRED_OK=0
    for tbl in $ALL_REQUIRED; do
        COUNT=$($MYSQL_UPGRADE "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_upgrade' AND table_name='$tbl'" 2>/dev/null || echo "0")
        if [ "$COUNT" -ge 1 ]; then
            REQUIRED_OK=$((REQUIRED_OK + 1))
        fi
    done
    if [ "$REQUIRED_OK" -eq 13 ]; then
        check "FlywayBaselineGuard 13张必需表全部存在(${REQUIRED_OK}/13)" "true"
    else
        check "FlywayBaselineGuard 13张必需表全部存在(${REQUIRED_OK}/13)" "false"
    fi

    # 2.2 验证无Flyway历史表(Upgrade路径: baseline后Flyway接管)
    FLYWAY_TABLE=$($MYSQL_UPGRADE "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='robot_home_upgrade' AND table_name='flyway_schema_history'" 2>/dev/null || echo "0")
    check "无flyway_schema_history表(Upgrade路径待baseline)" "$([ "$FLYWAY_TABLE" -eq 0 ] && echo true || echo false)"
else
    check "MySQL Upgrade容器运行中" "false"
fi
echo ""

# ========== 3. Redis ==========
echo "-- 3. Redis (localhost:16379) --"
if docker ps --format '{{.Names}}' | grep -q "robot-home-rehearsal-redis$"; then
    check "容器运行中" "true"
    PONG=$(docker exec robot-home-rehearsal-redis redis-cli ping 2>/dev/null || echo "FAIL")
    check "Redis PING→PONG" "$(echo "$PONG" | grep -q PONG && echo true || echo false)"
else
    check "Redis容器运行中" "false"
fi
echo ""

# ========== 4. MinIO ==========
echo "-- 4. MinIO (localhost:19000) --"
if docker ps --format '{{.Names}}' | grep -q "robot-home-rehearsal-minio$"; then
    check "容器运行中" "true"
    HEALTH=$(docker exec robot-home-rehearsal-minio curl -sf http://localhost:9000/minio/health/live 2>/dev/null && echo "OK" || echo "FAIL")
    check "MinIO健康检查" "$(echo "$HEALTH" | grep -q OK && echo true || echo false)"
else
    check "MinIO容器运行中" "false"
fi
echo ""

# ========== 5. Nginx ==========
echo "-- 5. Nginx (localhost:18080) --"
if docker ps --format '{{.Names}}' | grep -q "robot-home-rehearsal-nginx$"; then
    check "容器运行中" "true"
    NGINX_TEST=$(docker exec robot-home-rehearsal-nginx nginx -t 2>&1 || echo "FAIL")
    check "Nginx配置语法检查" "$(echo "$NGINX_TEST" | grep -q 'test is successful' && echo true || echo false)"
else
    check "Nginx容器运行中" "false"
fi
echo ""

# ========== 结果 ==========
echo "===================================="
echo " Runtime验证结果: PASS=$PASS FAIL=$FAIL SKIP=$SKIP"
echo "===================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "❌ 运行时验证存在FAIL项"
    echo "排查命令:"
    echo "  docker logs robot-home-rehearsal-mysql"
    echo "  docker logs robot-home-rehearsal-mysql-upgrade"
    echo "  docker logs robot-home-rehearsal-redis"
    echo "  docker logs robot-home-rehearsal-minio"
    echo "  docker logs robot-home-rehearsal-nginx"
    exit 1
else
    echo ""
    echo "✅ 运行时验证全部通过"
    exit 0
fi
