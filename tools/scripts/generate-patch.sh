#!/bin/bash
# Usage: generate-patch.sh <relative-file-path> [output-patch-name]
set -euo pipefail

REL_PATH="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DECOMPILED="$PROJECT_ROOT/src-decompiled/$REL_PATH"
PATCHED="$PROJECT_ROOT/src-patched/$REL_PATH"

[ -f "$DECOMPILED" ] || { echo "ERROR: Not found: $DECOMPILED" >&2; exit 1; }
[ -f "$PATCHED" ] || { echo "ERROR: Not found: $PATCHED" >&2; exit 1; }

! diff -u "$DECOMPILED" "$PATCHED" \
  | sed "1s|$DECOMPILED|a/$REL_PATH|" \
  | sed "2s|$PATCHED|b/$REL_PATH|" \
  | sed "1i diff --git a/$REL_PATH b/$REL_PATH"
