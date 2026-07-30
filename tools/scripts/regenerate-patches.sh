#!/bin/bash
# Regenerate all patches by diffing src-patched vs src-decompiled.
# Preserves manually-filled headers from existing patches (if any).
# Usage: regenerate-patches.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PATCHES_DIR="$PROJECT_ROOT/patches"
DECOMPILED="$PROJECT_ROOT/src-decompiled"
PATCHED="$PROJECT_ROOT/src-patched"
SCRIPT="$SCRIPT_DIR/generate-patch.sh"

echo "=== Saving existing patch headers ==="
declare -A HEADERS
for oldpatch in "$PATCHES_DIR"/*.patch; do
  [ -f "$oldpatch" ] || continue
  # extract class name from filename (strip NNNN- prefix and .patch suffix)
  base=$(basename "$oldpatch" .patch)
  classname="${base#*-}"
  # extract header (everything before standalone "---" line, inclusive)
  header=$(awk '/^---$/ { sep=1; print; next } sep==0 { print }' "$oldpatch")
  # only save if header was actually found (file has header section)
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
  bash "$SCRIPT" "$relpath" > "$PATCHES_DIR/$patchname"

  tmp="$PATCHES_DIR/.tmp-$patchname"
  if [ -n "${HEADERS["$filename"]:+x}" ]; then
    printf '%s\n' "${HEADERS["$filename"]}" > "$tmp"
  else
    cat > "$tmp" << HEADER
From: Folia Compatibility Patch
Subject: [PATCH ${num}] ${filename}: 

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