#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
REAL_MV="$(command -v mv)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-manifest-publication.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-manifest-publication.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

MOCK_BIN="$TMP_ROOT/bin"
ISOLATED_HOME="$TMP_ROOT/project/robot-home"
ARCHIVE_MOVE_STARTED="$TMP_ROOT/archive-move.started"
RELEASE_ARCHIVE_MOVE="$TMP_ROOT/archive-move.release"
OUTPUT="$TMP_ROOT/backup.log"
BACKUP_PID=""
mkdir -p "$MOCK_BIN" "$ISOLATED_HOME/scripts/db"
cp "$ROBOT_HOME/scripts/db/backup.sh" "$ISOLATED_HOME/scripts/db/backup.sh"
cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$ISOLATED_HOME/scripts/db/verify-backup.sh"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"

cleanup() {
    if [[ -n "$BACKUP_PID" ]] && kill -0 "$BACKUP_PID" 2>/dev/null; then
        touch "$RELEASE_ARCHIVE_MOVE"
        kill -TERM "$BACKUP_PID" 2>/dev/null || true
        wait "$BACKUP_PID" 2>/dev/null || true
    fi
    case "$TMP_ROOT" in
        /tmp/robot-home-manifest-publication.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

cat > "$MOCK_BIN/docker" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "ps" ]]; then
    printf 'robot-home-mysql\n'
elif [[ "$1" == "exec" ]]; then
    for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
        auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
        printf 'CREATE TABLE `%s` (id INT);\n' "$table"
    done
    printf 'INSERT INTO `article` VALUES (1);\n'
    printf -- '-- padding %s\n' "$(head -c 3000 /dev/urandom | base64 | tr -cd 'A-Za-z0-9')"
else
    exit 2
fi
MOCK
cat > "$MOCK_BIN/df" <<'MOCK'
#!/usr/bin/env bash
printf 'Filesystem 1M-blocks Used Available Use%% Mounted on\nmock 2048 1 2047 1%% /\n'
MOCK
cat > "$MOCK_BIN/mv" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "--" ]]; then shift; fi
if [[ "$#" -eq 2 && "$1" == */.robot_home_backup.* && "$2" == */robot_home_*.sql.gz ]]; then
    : > "$MOCK_ARCHIVE_MOVE_STARTED"
    while [[ ! -e "$MOCK_RELEASE_ARCHIVE_MOVE" ]]; do sleep 0.02; done
fi
exec "$REAL_MV" "$@"
MOCK
chmod +x "$MOCK_BIN/docker" "$MOCK_BIN/df" "$MOCK_BIN/mv"

PATH="$MOCK_BIN:$PATH" \
REAL_MV="$REAL_MV" \
MOCK_ARCHIVE_MOVE_STARTED="$ARCHIVE_MOVE_STARTED" \
MOCK_RELEASE_ARCHIVE_MOVE="$RELEASE_ARCHIVE_MOVE" \
MYSQL_ROOT_PASSWORD=test-only \
bash "$ISOLATED_HOME/scripts/db/backup.sh" > "$OUTPUT" 2>&1 &
BACKUP_PID=$!

for _ in $(seq 1 200); do
    [[ -e "$ARCHIVE_MOVE_STARTED" ]] && break
    sleep 0.02
done

if [[ ! -e "$ARCHIVE_MOVE_STARTED" ]]; then
    touch "$RELEASE_ARCHIVE_MOVE"
    wait "$BACKUP_PID" 2>/dev/null || true
    BACKUP_PID=""
    cat "$OUTPUT"
    echo 'FAIL: backup did not reach the final archive publication step.' >&2
    exit 1
fi

BACKUP_DIR="$ISOLATED_HOME/backups"
PUBLISHED_ARCHIVE="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'robot_home_*.sql.gz' -print -quit)"
PUBLISHED_MANIFEST="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'robot_home_*.sql.gz.manifest' -print -quit)"
if [[ -n "$PUBLISHED_ARCHIVE" || -z "$PUBLISHED_MANIFEST" ]]; then
    touch "$RELEASE_ARCHIVE_MOVE"
    cat "$OUTPUT"
    echo 'FAIL: final archive became visible before its verified SHA-256 manifest.' >&2
    exit 1
fi

touch "$RELEASE_ARCHIVE_MOVE"
set +e
wait "$BACKUP_PID"
BACKUP_EXIT=$?
set -e
BACKUP_PID=""
if [[ "$BACKUP_EXIT" -ne 0 ]]; then
    cat "$OUTPUT"
    echo "FAIL: backup failed after the manifest publication gate (exit=$BACKUP_EXIT)." >&2
    exit 1
fi

PUBLISHED_ARCHIVE="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'robot_home_*.sql.gz' -print -quit)"
if [[ -z "$PUBLISHED_ARCHIVE" || ! -f "$PUBLISHED_ARCHIVE.manifest" ]]; then
    cat "$OUTPUT"
    echo 'FAIL: completed backup is missing its archive or SHA-256 manifest.' >&2
    exit 1
fi
if ! bash "$ISOLATED_HOME/scripts/db/verify-backup.sh" "$PUBLISHED_ARCHIVE" > "$TMP_ROOT/verify.log" 2>&1; then
    cat "$TMP_ROOT/verify.log"
    echo 'FAIL: published archive and manifest did not verify together.' >&2
    exit 1
fi

echo 'PASS: the final archive appears only after its matching SHA-256 manifest is published.'
