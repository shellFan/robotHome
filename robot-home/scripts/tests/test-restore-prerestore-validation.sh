#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
REAL_GZIP="$(command -v gzip)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-prerestore-validation.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-prerestore-validation.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    if [[ -n "$RESTORE_PID" ]] && kill -0 "$RESTORE_PID" 2>/dev/null; then
        touch "$RELEASE_GZIP"
        kill -TERM "$RESTORE_PID" 2>/dev/null || true
        wait "$RESTORE_PID" 2>/dev/null || true
    fi
    case "$TMP_ROOT" in
        /tmp/robot-home-prerestore-validation.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

ISOLATED_HOME="$TMP_ROOT/project/robot-home"
MOCK_BIN="$TMP_ROOT/bin"
BACKUP_DIR="$ISOLATED_HOME/backups"
RESTORE_INPUT="$TMP_ROOT/restore-input.sql.gz"
SQL_FILE="$TMP_ROOT/restore-input.sql"
DOCKER_MARKER="$TMP_ROOT/mysql-import.called"
GZIP_STARTED="$TMP_ROOT/gzip.started"
RELEASE_GZIP="$TMP_ROOT/gzip.release"
FLOCK_LOG="$TMP_ROOT/flock.log"
OUTPUT="$TMP_ROOT/restore.log"
RESTORE_PID=""
mkdir -p "$ISOLATED_HOME/scripts/db" "$MOCK_BIN" "$BACKUP_DIR"
cp "$ROBOT_HOME/scripts/db/restore.sh" "$ISOLATED_HOME/scripts/db/restore.sh"
cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$ISOLATED_HOME/scripts/db/verify-backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done > "$SQL_FILE"
printf 'INSERT INTO `article` VALUES (1);\n' >> "$SQL_FILE"
gzip -c "$SQL_FILE" > "$RESTORE_INPUT"

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
    TABLES=(user robot_product article crawler_source enterprise_plan enterprise_subscription \
        auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06)
    if [[ "${MOCK_DUMP_MISSING_TABLE:-0}" == "1" ]]; then
        TABLES=(user robot_product article crawler_source enterprise_subscription \
            auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06 auxiliary_07)
    fi
    for table in "${TABLES[@]}"; do
        printf 'CREATE TABLE `%s` (id INT);\n' "$table"
    done
    printf 'INSERT INTO `article` VALUES (1);\n'
    printf -- '-- padding %s\n' "$(head -c 3000 /dev/urandom | base64 | tr -cd 'A-Za-z0-9')"
    exit 0
fi
if [[ "$IS_IMPORT" == "true" ]]; then
    : > "$MOCK_MYSQL_IMPORT_MARKER"
    exit 97
fi
exit 2
MOCK

cat > "$MOCK_BIN/gzip" <<'MOCK'
#!/usr/bin/env bash
if [[ "$#" -eq 0 && "${MOCK_GZIP_BLOCK:-0}" == "1" ]]; then
    : > "$MOCK_GZIP_STARTED"
    while [[ ! -e "$MOCK_RELEASE_GZIP" ]]; do sleep 0.02; done
fi
"$REAL_GZIP" "$@"
GZIP_EXIT=$?
if [[ "$GZIP_EXIT" -ne 0 ]]; then exit "$GZIP_EXIT"; fi
if [[ "$#" -eq 0 && "${MOCK_GZIP_FAIL:-0}" == "1" ]]; then exit 1; fi
exit 0
MOCK
chmod +x "$MOCK_BIN/docker" "$MOCK_BIN/gzip"

run_restore_case() {
    local expected="$1"
    shift
    rm -f "$FLOCK_LOG"
    rm -f "$DOCKER_MARKER" "$BACKUP_DIR"/pre_restore_*.sql.gz
    set +e
    printf 'YES\n' | env PATH="$MOCK_BIN:$PATH" REAL_GZIP="$REAL_GZIP" \
        MOCK_MYSQL_IMPORT_MARKER="$DOCKER_MARKER" MOCK_GZIP_STARTED="$GZIP_STARTED" \
        MOCK_RELEASE_GZIP="$RELEASE_GZIP" MOCK_FLOCK_LOG="$FLOCK_LOG" "$@" MYSQL_ROOT_PASSWORD=test-only \
        bash "$ISOLATED_HOME/scripts/db/restore.sh" "$RESTORE_INPUT" > "$OUTPUT" 2>&1
    RESTORE_EXIT=$?
    set -e
    if [[ "$RESTORE_EXIT" -eq 0 ]] || ! grep -Fq "$expected" "$OUTPUT"; then
        cat "$OUTPUT"
        echo "FAIL: restore did not stop with expected safeguard: $expected" >&2
        exit 1
    fi
    if [[ -e "$DOCKER_MARKER" ]]; then
        cat "$OUTPUT"
        echo 'FAIL: restore imported data after its pre-restore backup failed validation.' >&2
        exit 1
    fi
    if [[ -d "$BACKUP_DIR/.database-operation.lock" ]] || ! grep -Fq 'released' "$FLOCK_LOG"; then
        cat "$OUTPUT"
        echo 'FAIL: restore did not release its kernel lock after failure.' >&2
        exit 1
    fi
}

run_restore_case '恢复前备份失败' MOCK_GZIP_FAIL=1
echo 'PASS: a nonzero gzip exit after writing a valid stream blocks restore.'

run_restore_case 'enterprise_plan' MOCK_GZIP_FAIL=0 MOCK_DUMP_MISSING_TABLE=1
echo 'PASS: a pre-restore snapshot missing a required table blocks import.'

rm -f "$DOCKER_MARKER" "$GZIP_STARTED" "$RELEASE_GZIP" "$BACKUP_DIR"/pre_restore_*.sql.gz*
rm -f "$FLOCK_LOG"
printf 'YES\n' | env PATH="$MOCK_BIN:$PATH" REAL_GZIP="$REAL_GZIP" \
    MOCK_MYSQL_IMPORT_MARKER="$DOCKER_MARKER" MOCK_GZIP_STARTED="$GZIP_STARTED" \
    MOCK_RELEASE_GZIP="$RELEASE_GZIP" MOCK_FLOCK_LOG="$FLOCK_LOG" MOCK_GZIP_BLOCK=1 MOCK_DUMP_MISSING_TABLE=0 \
    MYSQL_ROOT_PASSWORD=test-only \
    bash "$ISOLATED_HOME/scripts/db/restore.sh" "$RESTORE_INPUT" > "$OUTPUT" 2>&1 &
RESTORE_PID=$!
for _ in $(seq 1 200); do
    [[ -e "$GZIP_STARTED" ]] && break
    sleep 0.02
done
if [[ ! -e "$GZIP_STARTED" ]]; then
    touch "$RELEASE_GZIP"
    wait "$RESTORE_PID" 2>/dev/null || true
    RESTORE_PID=""
    cat "$OUTPUT"
    echo 'FAIL: restore did not reach its pre-restore gzip pipeline.' >&2
    exit 1
fi

IN_PROGRESS_SNAPSHOT="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'pre_restore_*.sql.gz' -print -quit)"
if [[ -n "$IN_PROGRESS_SNAPSHOT" ]]; then
    touch "$RELEASE_GZIP"
    cat "$OUTPUT"
    echo 'FAIL: an in-progress pre-restore snapshot is visible under its final filename.' >&2
    exit 1
fi

touch "$RELEASE_GZIP"
set +e
wait "$RESTORE_PID"
RESTORE_EXIT=$?
set -e
RESTORE_PID=""
if [[ "$RESTORE_EXIT" -eq 0 ]]; then
    cat "$OUTPUT"
    echo 'FAIL: mocked database import unexpectedly succeeded.' >&2
    exit 1
fi
if ! grep -Fq 'released' "$FLOCK_LOG"; then
    cat "$OUTPUT"
    echo 'FAIL: restore did not release its kernel lock after the mocked import failed.' >&2
    exit 1
fi
FINAL_SNAPSHOT="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'pre_restore_*.sql.gz' -print -quit)"
if [[ -z "$FINAL_SNAPSHOT" || ! -f "$FINAL_SNAPSHOT.manifest" ]]; then
    cat "$OUTPUT"
    echo 'FAIL: completed pre-restore snapshot was not published with its manifest.' >&2
    exit 1
fi
if ! bash "$ISOLATED_HOME/scripts/db/verify-backup.sh" "$FINAL_SNAPSHOT" > "$TMP_ROOT/final-snapshot.log" 2>&1; then
    cat "$TMP_ROOT/final-snapshot.log"
    echo 'FAIL: the published pre-restore snapshot and manifest failed verification.' >&2
    exit 1
fi
echo 'PASS: pre-restore snapshots are published atomically with verified manifests.'
