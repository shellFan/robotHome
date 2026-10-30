#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROBOT_HOME="$(cd -- "$TEST_DIR/../.." && pwd)"
TMP_ROOT="$(mktemp -d /tmp/robot-home-backup-retention.XXXXXX)"

case "$TMP_ROOT" in
    /tmp/robot-home-backup-retention.*) ;;
    *) echo "Unexpected temp directory; refusing cleanup: $TMP_ROOT" >&2; exit 1 ;;
esac

cleanup() {
    case "$TMP_ROOT" in
        /tmp/robot-home-backup-retention.*) rm -rf -- "$TMP_ROOT" ;;
        *) echo "Unexpected cleanup target; refusing: $TMP_ROOT" >&2; return 1 ;;
    esac
}
trap cleanup EXIT

ISOLATED_HOME="$TMP_ROOT/project/robot-home"
mkdir -p "$ISOLATED_HOME/scripts/db"
cp "$ROBOT_HOME/scripts/db/backup.sh" "$ISOLATED_HOME/scripts/db/backup.sh"

for retain in 0 -1 abc 1.5; do
    OUTPUT="$TMP_ROOT/retain-${retain//[^a-zA-Z0-9]/_}.log"
    set +e
    env -u MYSQL_ROOT_PASSWORD bash "$ISOLATED_HOME/scripts/db/backup.sh" --retain "$retain" > "$OUTPUT" 2>&1
    EXIT_CODE=$?
    set -e

    if [[ "$EXIT_CODE" -eq 0 ]] || ! grep -Fq -- '--retain must be a positive integer' "$OUTPUT"; then
        cat "$OUTPUT"
        echo "FAIL: invalid retention value '$retain' was not rejected before backup setup." >&2
        exit 1
    fi
    if [[ -e "$ISOLATED_HOME/backups" ]]; then
        echo "FAIL: invalid retention value '$retain' created a backup directory." >&2
        exit 1
    fi
done

MOCK_BIN="$TMP_ROOT/bin"
mkdir -p "$MOCK_BIN"
source "$TEST_DIR/helpers/install-mock-flock.sh"
install_mock_flock "$MOCK_BIN"
cat > "$MOCK_BIN/df" <<'MOCK'
#!/usr/bin/env bash
printf 'Filesystem 1M-blocks Used Available Use%% Mounted on\nmock 2048 1 2047 1%% /\n'
MOCK
cat > "$MOCK_BIN/mysqldump" <<'MOCK'
#!/usr/bin/env bash
for table in user robot_product article crawler_source enterprise_plan enterprise_subscription \
    auxiliary_01 auxiliary_02 auxiliary_03 auxiliary_04 auxiliary_05 auxiliary_06; do
    printf 'CREATE TABLE `%s` (id INT);\n' "$table"
done
printf 'INSERT INTO `article` VALUES (1);\n'
printf -- '-- padding %s\n' "$(head -c 3000 /dev/urandom | base64 | tr -cd 'A-Za-z0-9')"
MOCK
chmod +x "$MOCK_BIN/df" "$MOCK_BIN/mysqldump"

run_retention_case() {
    local retain="$1"
    local initial_count="$2"
    local expected_count="$3"
    local label="$4"
    local case_home="$TMP_ROOT/project-$label/robot-home"
    local backup_dir="$case_home/backups"
    local output="$TMP_ROOT/$label.log"
    local i archive_count manifest_count

    mkdir -p "$case_home/scripts/db" "$backup_dir"
    cp "$ROBOT_HOME/scripts/db/backup.sh" "$case_home/scripts/db/backup.sh"
    cp "$ROBOT_HOME/scripts/db/verify-backup.sh" "$case_home/scripts/db/verify-backup.sh"
    for ((i = 1; i <= initial_count; i++)); do
        printf 'old backup %s\n' "$i" > "$backup_dir/robot_home_old_$i.sql.gz"
        printf 'sha256=old-%s\n' "$i" > "$backup_dir/robot_home_old_$i.sql.gz.manifest"
        touch -t 202001010000 "$backup_dir/robot_home_old_$i.sql.gz" \
            "$backup_dir/robot_home_old_$i.sql.gz.manifest"
    done

    if ! env PATH="$MOCK_BIN:$PATH" MYSQL_HOST=127.0.0.1 MYSQL_USER=test \
        MYSQL_PASSWORD=test MYSQL_DATABASE=robot_home \
        bash "$case_home/scripts/db/backup.sh" --standalone --retain "$retain" > "$output" 2>&1; then
        cat "$output"
        echo "FAIL: valid retention value '$retain' did not complete a mock backup." >&2
        exit 1
    fi

    archive_count=$(find "$backup_dir" -maxdepth 1 -type f -name 'robot_home_*.sql.gz' | wc -l | tr -d ' ')
    manifest_count=$(find "$backup_dir" -maxdepth 1 -type f -name 'robot_home_*.sql.gz.manifest' | wc -l | tr -d ' ')
    if [[ "$archive_count" -ne "$expected_count" || "$manifest_count" -ne "$expected_count" ]]; then
        cat "$output"
        echo "FAIL: $label expected $expected_count archive/manifest pairs; got $archive_count/$manifest_count." >&2
        exit 1
    fi
}

run_retention_case 08 10 8 leading-zero
run_retention_case 999999999999999999999999999999999999 3 4 oversized

echo 'PASS: zero, negative, decimal, and nonnumeric retention values fail before filesystem changes.'
echo 'PASS: leading-zero retention is decimal and oversized retention never overflows into deletion.'
