#!/bin/bash
set -euo pipefail

echo "Verifying patches..."

REGISTRY_PATH="patch-registry.json"
if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Error: patch-registry.json not found." >&2
  exit 1
fi

FAILED=0

num_patches=$(jq '.patches | length' "$REGISTRY_PATH")

for ((i=0; i<num_patches; i++)); do
  patch=$(jq -c ".patches[$i]" "$REGISTRY_PATH")
  id=$(echo "$patch" | jq -r '.id')
  name=$(echo "$patch" | jq -r '.name')
  patchFile=$(echo "$patch" | jq -r '.patchFile')
  
  status="✅"
  reason=""
  
  if [ ! -f "$patchFile" ]; then
    status="❌"
    reason="$reason Patch file $patchFile missing."
  fi
  
  num_files=$(echo "$patch" | jq '.files | length')
  for ((j=0; j<num_files; j++)); do
    f=$(echo "$patch" | jq -r ".files[$j]")
    patchedFile="src-patched/main/java/$f"
    if [ ! -f "$patchedFile" ]; then
      status="❌"
      reason="$reason Class file $f missing."
    fi
  done
  
  if [ "$status" = "❌" ]; then
    FAILED=1
  fi
  
  echo "$status [$id] $name$reason"
done

if [ $FAILED -ne 0 ]; then
  echo "Patch verification failed." >&2
  exit 1
else
  echo "All patches verified successfully."
fi
