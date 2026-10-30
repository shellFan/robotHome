#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-restore-validation.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-restore-validation.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    if [[ -n "$RESTORE_PID" ]] && kill -0 "$RESTORE_PID" 2>/dev/null; then
        kill -TERM "$RESTORE_PID" 2>/dev/null || true
        wait "$RESTORE_PID" 2>/dev/null || true
    fi
    exec 3>&- 3<&- 2>/dev/null || true
    case "$TMP_ROOT" in
        /tmp/robot-home-restore-validation.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

ISOLATED_HOME="$TMP_ROOT/project/robot-home"
MOCK_BIN="$TMP_ROOT/bin"
BACKUP_DIR="$ISOLATED_HOME/backups"
SQL_FILE="$TMP_ROOT/valid.sql"
ARCHIVE="$TMP_ROOT/valid.sql.gz"
MANIFEST="$ARCHIVE.manifest"
DOCKER_MARKER="$TMP_ROOT/docker.called"
OUTPUT="$TMP_ROOT/restore.log"
MUTATION_FIFO="$TMP_ROOT/restore-input.fifo"
FLOCK_LOG="$TMP_ROOT/flock.log"
RESTORE_PID=""
mkdir -p "$ISOLATED_HOME/scripts/db" "$MOCK_BIN" "$BACKUP_DIR"
cp "$ROBOT_HOME/scripts/db/restore.sh" "$ISOLATED_HOME/scripts/db/restore.sh"
cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$ISOLATED_HOME/scripts/db/verify-backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
: > "$MOCK_DOCKER_MARKER"
if [[ "$1" == "ps" ]]; then
    printf 'robot-home-mysql\n'
else
    exit 97
fi
MOCK
chmod +x "$MOCK_BIN/docker"

write_sql_with_data() {
    for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
        auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
        printf 'CREATE TABLE `%s` (id INT);\n' "$table"
    done > "$SQL_FILE"
    printf 'INSERT INTO `article` VALUES (1);\n' >> "$SQL_FILE"
}

run_restore() {
    local input="$1"
    shift
    set +e
    printf '%s\n' "$input" | env PATH="$MOCK_BIN:$PATH" MOCK_DOCKER_MARKER="$DOCKER_MARKER" \
        MOCK_FLOCK_LOG="$FLOCK_LOG" MYSQL_ROOT_PASSWORD=test-only bash "$ISOLATED_HOME/scripts/db/restore.sh" "$@" \
        > "$OUTPUT" 2>&1
    RESTORE_EXIT=$?
    set -e
}

write_sql_with_data
gzip -c "$SQL_FILE" > "$ARCHIVE"
ARCHIVE_SHA=$(sha256sum "$ARCHIVE" | awk '{ print $1 }')
cat > "$MANIFEST" <<MANIFEST
format=robot-home-backup-v1
database=robot_home
sha256=$ARCHIVE_SHA
created_at=20261030_000000
mode=docker
MANIFEST
WRONG_SHA=$(printf '%064d' 0)
sed "s/^sha256=.*/sha256=$WRONG_SHA/" "$MANIFEST" > "$MANIFEST.tmp"
mv "$MANIFEST.tmp" "$MANIFEST"

run_restore YES "$ARCHIVE"
if [[ "$RESTORE_EXIT" -eq 0 ]] || ! grep -Fq 'Manifest SHA-256 不匹配' "$OUTPUT"; then
    cat "$OUTPUT"
    echo 'FAIL: restore did not reject a SHA-256 mismatch before confirmation.' >&2
    exit 1
fi
if [[ -e "$DOCKER_MARKER" ]]; then
    cat "$OUTPUT"
    echo 'FAIL: restore invoked Docker for an archive with a mismatched manifest.' >&2
    exit 1
fi
rm -f "$MANIFEST"
echo 'PASS: restore rejects a mismatched manifest before any database command.'

for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done > "$SQL_FILE"
gzip -c "$SQL_FILE" > "$ARCHIVE"
run_restore YES "$ARCHIVE"
if [[ "$RESTORE_EXIT" -eq 0 ]] || ! grep -Fq '0 条 INSERT' "$OUTPUT"; then
    cat "$OUTPUT"
    echo 'FAIL: restore accepted an empty-data backup without an explicit override.' >&2
    exit 1
fi
if [[ -e "$DOCKER_MARKER" ]]; then
    cat "$OUTPUT"
    echo 'FAIL: restore invoked Docker for an empty-data backup without an override.' >&2
    exit 1
fi
echo 'PASS: restore rejects empty data unless explicitly enabled.'

run_restore NO --allow-empty-data "$ARCHIVE"
if [[ "$RESTORE_EXIT" -ne 0 ]] || ! grep -Fq '已取消恢复' "$OUTPUT"; then
    cat "$OUTPUT"
    echo 'FAIL: explicit empty-data override did not reach the normal confirmation gate.' >&2
    exit 1
fi
if [[ -e "$DOCKER_MARKER" || -d "$BACKUP_DIR/.database-operation.lock" ]]; then
    cat "$OUTPUT"
    echo 'FAIL: declining the restore after validation touched Docker or left an operation lock.' >&2
    exit 1
fi
echo 'PASS: explicit empty-data override still requires affirmative restore confirmation.'

write_sql_with_data
gzip -c "$SQL_FILE" > "$ARCHIVE"
ARCHIVE_SHA=$(sha256sum "$ARCHIVE" | awk '{ print $1 }')
cat > "$MANIFEST" <<MANIFEST
format=robot-home-backup-v1
database=robot_home
sha256=$ARCHIVE_SHA
created_at=20261030_000000
mode=docker
MANIFEST
mkfifo "$MUTATION_FIFO"
exec 3<> "$MUTATION_FIFO"
rm -f "$DOCKER_MARKER"
env PATH="$MOCK_BIN:$PATH" MOCK_DOCKER_MARKER="$DOCKER_MARKER" \
    MOCK_FLOCK_LOG="$FLOCK_LOG" MYSQL_ROOT_PASSWORD=test-only bash "$ISOLATED_HOME/scripts/db/restore.sh" "$ARCHIVE" \
    < "$MUTATION_FIFO" > "$OUTPUT" 2>&1 &
RESTORE_PID=$!
for _ in $(seq 1 200); do
    grep -Fq '此操作将覆盖目标数据库中的现有数据' "$OUTPUT" 2>/dev/null && break
    sleep 0.02
done
if ! grep -Fq '此操作将覆盖目标数据库中的现有数据' "$OUTPUT" 2>/dev/null; then
    kill -TERM "$RESTORE_PID" 2>/dev/null || true
    wait "$RESTORE_PID" 2>/dev/null || true
    RESTORE_PID=""
    exec 3>&- 3<&-
    cat "$OUTPUT"
    echo 'FAIL: restore did not reach its confirmation gate for the mutation test.' >&2
    exit 1
fi

sed "s/^sha256=.*/sha256=$WRONG_SHA/" "$MANIFEST" > "$MANIFEST.tmp"
mv "$MANIFEST.tmp" "$MANIFEST"
printf 'YES\n' >&3
set +e
wait "$RESTORE_PID"
RESTORE_EXIT=$?
set -e
RESTORE_PID=""
exec 3>&- 3<&-
rm -f "$MUTATION_FIFO"
if [[ "$RESTORE_EXIT" -eq 0 ]] || ! grep -Fq 'Manifest SHA-256 不匹配' "$OUTPUT"; then
    cat "$OUTPUT"
    echo 'FAIL: restore did not revalidate the backup after the confirmation wait.' >&2
    exit 1
fi
if [[ -e "$DOCKER_MARKER" || -d "$BACKUP_DIR/.database-operation.lock" ]] || ! grep -Fq 'released' "$FLOCK_LOG"; then
    cat "$OUTPUT"
    echo 'FAIL: mutated backup reached a database command or restore failed to release its kernel lock.' >&2
    exit 1
fi
echo 'PASS: restore rechecks the archive under its lock after user confirmation and before database commands.'
