#!/usr/bin/env bash

install_mock_flock() {
    local mock_bin="$1"

    cat > "$mock_bin/flock" <<'MOCK'
#!/usr/bin/env bash
case "${1:-}" in
    -n)
        if [[ "${MOCK_FLOCK_BUSY:-0}" == "1" ]]; then
            if [[ -n "${MOCK_FLOCK_LOG:-}" ]]; then printf 'busy\n' >> "$MOCK_FLOCK_LOG"; fi
            exit 1
        fi
        if [[ -n "${MOCK_FLOCK_LOG:-}" ]]; then printf 'acquired\n' >> "$MOCK_FLOCK_LOG"; fi
        exit 0
        ;;
    -u)
        if [[ -n "${MOCK_FLOCK_LOG:-}" ]]; then printf 'released\n' >> "$MOCK_FLOCK_LOG"; fi
        exit 0
        ;;
    *)
        echo "unexpected flock arguments: $*" >&2
        exit 2
        ;;
esac
MOCK
    chmod +x "$mock_bin/flock"
}
