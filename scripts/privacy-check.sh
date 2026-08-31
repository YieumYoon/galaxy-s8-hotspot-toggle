#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Keep the original private identifiers out of the repository text itself while
# still checking for accidental reintroduction in generated changes.
original_name_pattern='jun''su'
mac_home_pattern='/''Users/'

forbidden_files="$(find . -type f \
    -not -path './.git/*' \
    -not -path './build/*' \
    -not -path './dist/*' \
    \( -name '*.apk' -o -name '*.aab' -o -name '*.apks' -o -name '*.idsig' \
       -o -name '*.keystore' -o -name '*.jks' -o -name '*.odex' -o -name '*.vdex' \
       -o -name '*.pem' -o -name '*.key' \) -print)"

if [[ -n "$forbidden_files" ]]; then
    echo "Forbidden binary or credential files found:" >&2
    echo "$forbidden_files" >&2
    exit 1
fi

if rg -n -i --hidden \
    -g '!.git/**' \
    -g '!build/**' \
    -g '!dist/**' \
    -g '!scripts/privacy-check.sh' \
    -e "com\\.${original_name_pattern}" \
    -e "${mac_home_pattern}${original_name_pattern}" \
    -e "${mac_home_pattern}[^/[:space:]]+" \
    -e '[A-Za-z]:\\Users\\' \
    -e 'gh[oprsu]_[A-Za-z0-9]{20,}' \
    -e 'AKIA[0-9A-Z]{16}' \
    -e 'AIza[0-9A-Za-z_-]{30,}' \
    -e 'xox[baprs]-[0-9A-Za-z-]+' \
    -e 'BEGIN [A-Z ]*PRIVATE KEY'; then
    echo "Potential personal data or secret found." >&2
    exit 1
fi

echo "Privacy check passed."
