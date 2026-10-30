#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-restore-input-snapshot.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-restore-input-snapshot.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    case "$TMP_ROOT" in
        /tmp/robot-home-restore-input-snapshot.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

ISOLATED_HOME="$TMP_ROOT/project/robot-home"
MOCK_BIN="$TMP_ROOT/bin"
BACKUP_DIR="$ISOLATED_HOME/backups"
SOURCE_ARCHIVE="$TMP_ROOT/restore-input.sql.gz"
REPLACEMENT_ARCHIVE="$TMP_ROOT/replacement.sql.gz"
ORIGINAL_SQL="$TMP_ROOT/original.sql"
REPLACEMENT_SQL="$TMP_ROOT/replacement.sql"
IMPORTED_SQL="$TMP_ROOT/imported.sql"
OUTPUT="$TMP_ROOT/restore.log"
mkdir -p "$ISOLATED_HOME/scripts/db" "$MOCK_BIN"
cp "$ROBOT_HOME/scripts/db/restore.sh" "$ISOLATED_HOME/scripts/db/restore.sh"
cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$ISOLATED_HOME/scripts/db/verify-backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

write_archive_sql() {
    local destination="$1"
    local marker="$2"
    for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
        auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
        printf 'CREATE TABLE `%s` (id INT);\n' "$table"
    done > "$destination"
    printf "INSERT INTO \`article\` VALUES ('%s');\n" "$marker" >> "$destination"
}

write_archive_sql "$ORIGINAL_SQL" original
write_archive_sql "$REPLACEMENT_SQL" replacement
gzip -c "$ORIGINAL_SQL" > "$SOURCE_ARCHIVE"
gzip -c "$REPLACEMENT_SQL" > "$REPLACEMENT_ARCHIVE"

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "ps" ]]; then
    printf 'robot-home-mysql\n'
    exit 0
fi
if [[ "$1" != "exec" ]]; then exit 2; fi

IS_DUMP=false
IS_IMPORT=false
for argument in "$@"; do
    [[ "$argument" == "mysqldump" ]] && IS_DUMP=true
    [[ "$argument" == "mysql" ]] && IS_IMPORT=true
done

if [[ "$IS_DUMP" == "true" ]]; then
    cp "$MOCK_REPLACEMENT_ARCHIVE" "$MOCK_SOURCE_ARCHIVE"
    for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
        auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
        printf 'CREATE TABLE `%s` (id INT);\n' "$table"
    done
    printf 'INSERT INTO `article` VALUES (1);\n'
    printf -- '-- padding %s\n' "$(head -c 4096 /dev/urandom | base64 | tr -cd 'A-Za-z0-9')"
    exit 0
fi
if [[ "$IS_IMPORT" == "true" ]]; then
    cat > "$MOCK_IMPORTED_SQL"
    exit 0
fi
exit 2
MOCK
chmod +x "$MOCK_BIN/docker"

set +e
printf 'YES\n' | env PATH="$MOCK_BIN:$PATH" MOCK_SOURCE_ARCHIVE="$SOURCE_ARCHIVE" \
    MOCK_REPLACEMENT_ARCHIVE="$REPLACEMENT_ARCHIVE" MOCK_IMPORTED_SQL="$IMPORTED_SQL" \
    MYSQL_ROOT_PASSWORD=test-only bash "$ISOLATED_HOME/scripts/db/restore.sh" "$SOURCE_ARCHIVE" \
    > "$OUTPUT" 2>&1
RESTORE_EXIT=$?
set -e

if [[ "$RESTORE_EXIT" -ne 0 ]]; then
    cat "$OUTPUT"
    echo 'FAIL: restore failed before the input replacement check.' >&2
    exit 1
fi
if ! grep -Fq "VALUES ('original')" "$IMPORTED_SQL" || grep -Fq "VALUES ('replacement')" "$IMPORTED_SQL"; then
    cat "$IMPORTED_SQL" 2>/dev/null || true
    echo 'FAIL: restore imported bytes from the mutable source path instead of the verified snapshot.' >&2
    exit 1
fi

echo 'PASS: restore imports the exact archive bytes that it verified, even if the source path changes later.'
