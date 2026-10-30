#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
ROBOT_HOME="$(cd "$TEST_DIR/../.." && pwd)"
BACKUP_SCRIPT="$ROBOT_HOME/scripts/db/backup.sh"
TMP_PARENT="/tmp"
TMP_ROOT="$(mktemp -d "$TMP_PARENT/robot-home-backup-gzip-failure.XXXXXX")"
case "$TMP_ROOT" in
    "$TMP_PARENT"/robot-home-backup-gzip-failure.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac
MOCK_BIN="$TMP_ROOT/bin"
ISOLATED_HOME="$TMP_ROOT/robot-home"
mkdir -p "$MOCK_BIN" "$ISOLATED_HOME/scripts/db"
cp "$BACKUP_SCRIPT" "$ISOLATED_HOME/scripts/db/backup.sh"
trap 'rm -rf -- "$TMP_ROOT"' EXIT

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "ps" ]]; then
    echo "robot-home-mysql"
elif [[ "$1" == "exec" ]]; then
    head -c 2048 /dev/zero
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
cat >/dev/null
head -c 2048 /dev/zero
exit 1
MOCK

chmod +x "$MOCK_BIN/docker" "$MOCK_BIN/df" "$MOCK_BIN/gzip"

set +e
PATH="$MOCK_BIN:$PATH" MYSQL_ROOT_PASSWORD=test-only "$ISOLATED_HOME/scripts/db/backup.sh" >"$TMP_ROOT/output.log" 2>&1
BACKUP_EXIT=$?
set -e

BACKUP_FILES="$(find "$ISOLATED_HOME/backups" -maxdepth 1 -type f -name 'robot_home_*.sql.gz' -print 2>/dev/null || true)"
if [[ "$BACKUP_EXIT" -ne 0 && -z "$BACKUP_FILES" ]]; then
    echo "PASS: backup.sh rejects gzip failure and removes the incomplete artifact."
else
    cat "$TMP_ROOT/output.log"
    echo "FAIL: expected a nonzero exit and no backup artifact (exit=$BACKUP_EXIT, files=$BACKUP_FILES)." >&2
    exit 1
fi
