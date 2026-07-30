#!/bin/bash
# Inject folia-supported: true into plugin.yml
# Usage: inject-folia-support.sh <path/to/plugin.yml>
set -euo pipefail

FILE="$1"
[ -f "$FILE" ] || { echo "Error: file not found: $FILE" >&2; exit 1; }

if grep -q '^folia-supported:' "$FILE"; then
  # Line exists uncommented → force value to true
  sed -i 's/^folia-supported:.*/folia-supported: true/' "$FILE"
  echo "  → Set folia-supported: true (was present, forced)"
else
  # No uncommented line → insert after api-version
  # Ignore commented #folia-supported lines
  if grep -q '^api-version:' "$FILE"; then
    sed -i '/^api-version:/a\folia-supported: true' "$FILE"
    echo "  → Injected folia-supported: true into plugin.yml"
  else
    echo "Warning: no api-version line found in $FILE" >&2
    exit 1
  fi
fi
