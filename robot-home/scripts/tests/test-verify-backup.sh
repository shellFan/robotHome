#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
VERIFY_SCRIPT="$ROBOT_HOME/scripts/db/verify-backup.sh"
TMP_ROOT="$(mktemp -d /tmp/robot-home-verify-backup.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-verify-backup.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    case "$TMP_ROOT" in
        /tmp/robot-home-verify-backup.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

VALID_SQL="$TMP_ROOT/valid.sql"
VALID_BACKUP="$TMP_ROOT/valid.sql.gz"
MISSING_SQL="$TMP_ROOT/missing.sql"
MISSING_BACKUP="$TMP_ROOT/missing.sql.gz"
CORRUPT_BACKUP="$TMP_ROOT/corrupt.sql.gz"
VERIFY_OUTPUT="$TMP_ROOT/verify.log"

for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done > "$VALID_SQL"
printf 'INSERT INTO `article` VALUES (1);\n' >> "$VALID_SQL"
{
    printf '/*'
    head -c 33554432 /dev/zero | tr '\0' 'x'
    printf '*/\n'
} >> "$VALID_SQL"
gzip -c "$VALID_SQL" > "$VALID_BACKUP"

if bash "$VERIFY_SCRIPT" "$VALID_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    if ! grep -Fq '[3/4] 关键表检查... PASS' "$VERIFY_OUTPUT"; then
        cat "$VERIFY_OUTPUT"
        echo 'FAIL: valid large backup did not report all required tables.' >&2
        exit 1
    fi
    echo 'PASS: valid large backup passes the required-table check.'
else
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: valid large backup was rejected.' >&2
    exit 1
fi

for table in user robot_product article crawler_source enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done > "$MISSING_SQL"
printf 'INSERT INTO `article` VALUES (1);\n' >> "$MISSING_SQL"
gzip -c "$MISSING_SQL" > "$MISSING_BACKUP"

if bash "$VERIFY_SCRIPT" "$MISSING_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: backup missing a required table was accepted.' >&2
    exit 1
elif ! grep -Fq 'enterprise_plan' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not identify the missing required table.' >&2
    exit 1
fi

echo 'PASS: backup missing a required table is rejected and identified.'

head -c -8 "$VALID_BACKUP" > "$CORRUPT_BACKUP"
if bash "$VERIFY_SCRIPT" "$CORRUPT_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: corrupted backup was accepted.' >&2
    exit 1
fi
if ! grep -Fq 'FAIL（SQL 内容无法完整读取）' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not report the failed SQL scan.' >&2
    exit 1
fi
if ! grep -Fq '=== 验证结果:' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier skipped the final result summary for a corrupted backup.' >&2
    exit 1
fi
if ! grep -Fq '=== 验证结果: 2 PASS / 2 FAIL ===' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not count the unreadable SQL scan as a failure.' >&2
    exit 1
fi

echo 'PASS: corrupted backup is rejected and the verifier prints its final summary.'
