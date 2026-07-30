#!/bin/bash
set -euo pipefail

echo "=== Copying src-decompiled to src-patched ==="
rm -rf src-patched
cp -a src-decompiled src-patched
echo "Done: $(find src-patched -name '*.java' | wc -l) files"

echo ""
echo "=== Applying patches ==="
ok=0
fail=0
for p in patches/*.patch; do
  name=$(basename "$p")
  if git apply --directory=src-patched "$p" 2>/dev/null; then
    echo "  OK: $name"
    ok=$((ok+1))
  else
    echo "  FAIL: $name"
    fail=$((fail+1))
  fi
done
echo ""
echo "Result: $ok applied, $fail failed"
