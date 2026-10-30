#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
REAL_GZIP="$(command -v gzip)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-backup-atomic.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-backup-atomic.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

MOCK_BIN="$TMP_ROOT/bin"
ISOLATED_HOME="$TMP_ROOT/project/robot-home"
STARTED="$TMP_ROOT/gzip.started"
RELEASE="$TMP_ROOT/gzip.release"
mkdir -p "$MOCK_BIN" "$ISOLATED_HOME/scripts/db"
cp "$ROBOT_HOME/scripts/db/backup.sh" "$ISOLATED_HOME/scripts/db/backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

BACKUP_PID=""
cleanup() {
    if [[ -n "$BACKUP_PID" ]] && kill -0 "$BACKUP_PID" 2>/dev/null; then
        touch "$RELEASE"
        kill -TERM "$BACKUP_PID" 2>/dev/null || true
        wait "$BACKUP_PID" 2>/dev/null || true
    fi
    case "$TMP_ROOT" in
        /tmp/robot-home-backup-atomic.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "ps" ]]; then
    echo "robot-home-mysql"
elif [[ "$1" == "exec" ]]; then
    head -c 8192 /dev/urandom
else
    exit 2
fi
MOCK

cat > "$MOCK_BIN/df" <<'MOCK'
#!/usr/bin/env bash
printf 'Filesystem 1M-blocks Used Available Use%% Mounted on\nmock 2048 1 2047 1%% /\n'
MOCK

cat > "$MOCK_BIN/gzip" <<'MOCK'
#!/usr/bin/env bash
if [[ $# -eq 0 && -n "${MOCK_GZIP_STARTED:-}" ]]; then
    : > "$MOCK_GZIP_STARTED"
    while [[ ! -e "$MOCK_GZIP_RELEASE" ]]; do sleep 0.02; done
    cat >/dev/null
    exit 1
fi
exec "$REAL_GZIP" "$@"
MOCK

chmod +x "$MOCK_BIN/docker" "$MOCK_BIN/df" "$MOCK_BIN/gzip"
PATH="$MOCK_BIN:$PATH" \
REAL_GZIP="$REAL_GZIP" \
MOCK_GZIP_STARTED="$STARTED" \
MOCK_GZIP_RELEASE="$RELEASE" \
MYSQL_ROOT_PASSWORD=test-only \
bash "$ISOLATED_HOME/scripts/db/backup.sh" > "$TMP_ROOT/output.log" 2>&1 &
BACKUP_PID=$!

for _ in $(seq 1 100); do
    [[ -e "$STARTED" ]] && break
    sleep 0.02
done

if [[ ! -e "$STARTED" ]]; then
    touch "$RELEASE"
    wait "$BACKUP_PID" 2>/dev/null || true
    BACKUP_PID=""
    cat "$TMP_ROOT/output.log"
    echo 'FAIL: mocked gzip did not reach the blocked write phase.' >&2
    exit 1
fi

PARTIAL_FINAL="$(find "$ISOLATED_HOME/backups" -maxdepth 1 -type f -name 'robot_home_*.sql.gz' -print -quit)"
touch "$RELEASE"
set +e
wait "$BACKUP_PID"
BACKUP_EXIT=$?
set -e
BACKUP_PID=""

if [[ -z "$PARTIAL_FINAL" && "$BACKUP_EXIT" -ne 0 ]]; then
    echo 'PASS: an in-progress backup is never visible under its final filename.'
else
    cat "$TMP_ROOT/output.log"
    echo "FAIL: partial final artifact was visible or backup unexpectedly succeeded (file=$PARTIAL_FINAL, exit=$BACKUP_EXIT)." >&2
    exit 1
fi
