#!/bin/bash
set -euo pipefail

if [ ! -d "src-decompiled" ]; then
  echo "Error: src-decompiled directory not found. Please decompile first." >&2
  exit 1
fi

echo "Cleaning and copying decompiled sources to src-patched/main/java..."
TARGET_DIR="src-patched/main/java"
rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"
cp -r src-decompiled/* "$TARGET_DIR/"

if [ ! -d "patches" ]; then
  echo "No patches directory found."
  exit 0
fi

# Find and sort patch files naturally
shopt -s nullglob
patches=(patches/*.patch)

if [ ${#patches[@]} -eq 0 ]; then
  echo "No patch files found in patches/."
  exit 0
fi

# Sort patches naturally
sorted_patches=($(printf '%s\n' "${patches[@]}" | sort))

echo "Applying ${#sorted_patches[@]} patches..."

successCount=0
for patchPath in "${sorted_patches[@]}"; do
  patch=$(basename "$patchPath")
  echo "Applying $patch..."
  
  if ! git apply --directory=src-patched/main/java --ignore-whitespace "$patchPath"; then
    echo "Error: Failed to apply $patch" >&2
    exit 1
  fi
  successCount=$((successCount + 1))
done

echo "$successCount patches applied successfully."
