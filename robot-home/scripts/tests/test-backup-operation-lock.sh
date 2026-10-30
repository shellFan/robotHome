#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-operation-lock.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-operation-lock.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    case "$TMP_ROOT" in
        /tmp/robot-home-operation-lock.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

ISOLATED_HOME="$TMP_ROOT/project/robot-home"
MOCK_BIN="$TMP_ROOT/bin"
BACKUP_DIR="$ISOLATED_HOME/backups"
ARCHIVE="$TMP_ROOT/restore-test.sql.gz"
DOCKER_MARKER="$TMP_ROOT/docker.called"
FLOCK_LOG="$TMP_ROOT/flock.log"
OUTPUT="$TMP_ROOT/operation.log"
mkdir -p "$ISOLATED_HOME/scripts/db" "$MOCK_BIN"
cp "$ROBOT_HOME/scripts/db/backup.sh" "$ISOLATED_HOME/scripts/db/backup.sh"
cp "$ROBOT_HOME/scripts/db/restore.sh" "$ISOLATED_HOME/scripts/db/restore.sh"
cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$ISOLATED_HOME/scripts/db/verify-backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done | gzip > "$ARCHIVE"
printf 'INSERT INTO `article` VALUES (1);\n' | gzip -c >> "$ARCHIVE"

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
: > "$MOCK_DOCKER_MARKER"
if [[ "$1" == "ps" ]]; then
    printf 'robot-home-mysql\n'
    exit 0
fi
exit 97
MOCK
cat > "$MOCK_BIN/df" <<'MOCK'
#!/usr/bin/env bash
printf 'Filesystem 1M-blocks Used Available Use%% Mounted on\nmock 2048 1 2047 1%% /\n'
MOCK
chmod +x "$MOCK_BIN/docker" "$MOCK_BIN/df"

run_busy_case() {
    local label="$1"
    shift
    local exit_code=0

    rm -f "$DOCKER_MARKER"
    set +e
    printf 'YES\n' | env PATH="$MOCK_BIN:$PATH" MOCK_FLOCK_BUSY=1 MOCK_FLOCK_LOG="$FLOCK_LOG" \
        MOCK_DOCKER_MARKER="$DOCKER_MARKER" MYSQL_ROOT_PASSWORD=test-only \
        bash "$@" > "$OUTPUT" 2>&1
    exit_code=$?
    set -e

    if [[ "$exit_code" -eq 0 ]] || ! grep -Fq '另一个备份或恢复操作正在运行' "$OUTPUT"; then
        cat "$OUTPUT"
        echo "FAIL: $label did not refuse an already-held shared operation lock." >&2
        exit 1
    fi
    if [[ -e "$DOCKER_MARKER" ]]; then
        cat "$OUTPUT"
        echo "FAIL: $label invoked Docker while another database operation held the lock." >&2
        exit 1
    fi
}

run_busy_case backup "$ISOLATED_HOME/scripts/db/backup.sh"
run_busy_case restore "$ISOLATED_HOME/scripts/db/restore.sh" "$ARCHIVE"

BUSY_COUNT="$(grep -c '^busy$' "$FLOCK_LOG" || true)"
if [[ "$BUSY_COUNT" -ne 2 ]]; then
    cat "$FLOCK_LOG" 2>/dev/null || true
    echo "FAIL: expected backup and restore to consult the shared lock, saw $BUSY_COUNT busy results." >&2
    exit 1
fi

echo 'PASS: backup and restore both refuse a held shared kernel lock before database commands.'
