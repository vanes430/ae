#!/bin/bash
# Generate patches. Two modes:
#   generate_patches.sh                     — bulk: regenerate ALL patches from diff (preserves headers)
#   generate_patches.sh <relative-path>     — single: diff one file, print patch to stdout
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PATCHES_DIR="$PROJECT_ROOT/patches"
DECOMPILED="$PROJECT_ROOT/src-decompiled"
PATCHED="$PROJECT_ROOT/src-patched"

# ── Helper: generate raw diff for one file ──
gen_diff() {
  local rel="$1"
  local a="$DECOMPILED/$rel" b="$PATCHED/$rel"
  [ -f "$a" ] || { echo "ERROR: Not found: $a" >&2; exit 1; }
  [ -f "$b" ] || { echo "ERROR: Not found: $b" >&2; exit 1; }
  ! diff -u "$a" "$b" \
    | sed "1s|$a|a/$rel|" \
    | sed "2s|$b|b/$rel|" \
    | sed "1i diff --git a/$rel b/$rel"
}

# ── Single-file mode ──
if [ $# -eq 1 ]; then
  gen_diff "$1"
  exit 0
fi

[ $# -eq 0 ] || { echo "Usage: generate_patches.sh [<relative-path>]" >&2; exit 1; }

# ── Bulk mode ──

# Save old headers
echo "=== Saving existing patch headers ==="
declare -A HEADERS
for oldpatch in "$PATCHES_DIR"/*.patch; do
  [ -f "$oldpatch" ] || continue
  base=$(basename "$oldpatch" .patch)
  classname="${base#*-}"
  header=$(awk '/^---$/ { sep=1; print; next } sep==0 { print }' "$oldpatch" | sed -E 's/\[PATCH( [0-9]+)?\] //' | grep -v '^Date: ')
  if echo "$header" | grep -q '^---$'; then
    HEADERS["$classname"]="$header"
    echo "  saved header for $classname"
  fi
done

echo "=== Removing old patches ==="
rm -f "$PATCHES_DIR"/*.patch

echo "=== Finding differing files ==="
! diff -rq "$DECOMPILED" "$PATCHED" 2>/dev/null \
  | grep "^Files" \
  | sed 's/^Files //; s/ and.* differ$//' \
  | sed 's|.*/src-decompiled/||' \
  | sort -t/ -k5 > "$PATCHES_DIR/.differing-files.txt"

total=$(wc -l < "$PATCHES_DIR/.differing-files.txt")
i=0

while read -r relpath; do
  i=$((i+1))
  filename=$(basename "$relpath" .java)
  num=$(printf "%04d" $i)
  patchname="${num}-${filename}.patch"

  echo "  [$num/$total] $relpath → $patchname"
  gen_diff "$relpath" > "$PATCHES_DIR/$patchname"

  tmp="$PATCHES_DIR/.tmp-$patchname"
  if [ -n "${HEADERS["$filename"]:+x}" ]; then
    printf '%s\n' "${HEADERS["$filename"]}" > "$tmp"
    sed -i "2i Date: $(date -R)" "$tmp"
  else
    cat > "$tmp" << HEADER
From: Shimazu <vanessimbolon2020@gmail.com>
Date: $(date -R)
Subject: ${filename}: 

AE PATCH REASON: 
AE PATCH FIX: 
AE PATCH RISK: 

---
HEADER
  fi
  cat "$PATCHES_DIR/$patchname" >> "$tmp"
  mv "$tmp" "$PATCHES_DIR/$patchname"
done < "$PATCHES_DIR/.differing-files.txt"
rm -f "$PATCHES_DIR/.differing-files.txt"

echo ""
echo "Done: $i patches generated"
