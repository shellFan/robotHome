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
VALID_MANIFEST="$TMP_ROOT/valid.sql.gz.manifest"
MISSING_SQL="$TMP_ROOT/missing.sql"
MISSING_BACKUP="$TMP_ROOT/missing.sql.gz"
CORRUPT_BACKUP="$TMP_ROOT/corrupt.sql.gz"
EMPTY_SQL="$TMP_ROOT/empty.sql"
EMPTY_BACKUP="$TMP_ROOT/empty.sql.gz"
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
    if ! grep -Fq '[4/5] 关键表检查... PASS' "$VERIFY_OUTPUT"; then
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

VALID_SHA=$(sha256sum "$VALID_BACKUP" | awk '{ print $1 }')
cat > "$VALID_MANIFEST" <<MANIFEST
format=robot-home-backup-v1
database=robot_home
sha256=$VALID_SHA
created_at=20261030_000000
mode=standalone
MANIFEST
if ! bash "$VERIFY_SCRIPT" "$VALID_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: valid backup manifest was rejected.' >&2
    exit 1
fi

WRONG_SHA=$(printf '%064d' 0)
sed "s/^sha256=.*/sha256=$WRONG_SHA/" "$VALID_MANIFEST" > "$VALID_MANIFEST.tmp"
mv "$VALID_MANIFEST.tmp" "$VALID_MANIFEST"
if bash "$VERIFY_SCRIPT" "$VALID_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: backup with a mismatched SHA-256 manifest was accepted.' >&2
    exit 1
fi
if ! grep -Fq 'Manifest SHA-256 不匹配' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not identify the mismatched manifest digest.' >&2
    exit 1
fi
rm -f "$VALID_MANIFEST"
echo 'PASS: a mismatched SHA-256 manifest is rejected.'

NEW_FORMAT_BACKUP="$TMP_ROOT/robot_home_20261030_123456_1234_5678.sql.gz"
cp "$VALID_BACKUP" "$NEW_FORMAT_BACKUP"
if bash "$VERIFY_SCRIPT" "$NEW_FORMAT_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: a new-format backup without its manifest was accepted.' >&2
    exit 1
fi
if ! grep -Fq '新格式备份缺少 SHA-256 Manifest' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not require the manifest for a new-format backup.' >&2
    exit 1
fi
if ! bash "$VERIFY_SCRIPT" "$VALID_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: legacy backup without a manifest was no longer accepted.' >&2
    exit 1
fi
echo 'PASS: new-format backups require a manifest while legacy backups remain supported.'

for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done > "$EMPTY_SQL"
gzip -c "$EMPTY_SQL" > "$EMPTY_BACKUP"

if bash "$VERIFY_SCRIPT" "$EMPTY_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: structure-only backup was accepted without an explicit override.' >&2
    exit 1
fi
if ! grep -Fq '0 条 INSERT' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not explain that the backup contains no data rows.' >&2
    exit 1
fi
if ! bash "$VERIFY_SCRIPT" --allow-empty-data "$EMPTY_BACKUP" > "$VERIFY_OUTPUT" 2>&1; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: explicit empty-data override was rejected.' >&2
    exit 1
fi
if ! grep -Fq '[5/5] 数据行数统计... PASS（显式允许空数据）' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: explicit empty-data override was not recorded in the verification output.' >&2
    exit 1
fi
echo 'PASS: structure-only backup fails closed unless empty data is explicitly allowed.'

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
if ! grep -Fq '=== 验证结果: 0 PASS / 4 FAIL ===' "$VERIFY_OUTPUT"; then
    cat "$VERIFY_OUTPUT"
    echo 'FAIL: verifier did not fail each check that reads the corrupted SQL stream.' >&2
    exit 1
fi

echo 'PASS: corrupted backup is rejected and the verifier prints its final summary.'
