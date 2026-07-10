#!/bin/bash
set -euo pipefail

REGISTRY_PATH="patch-registry.json"
if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Error: patch-registry.json not found." >&2
  exit 1
fi

# Clear old patches
rm -rf patches
mkdir -p patches

num_patches=$(jq '.patches | length' "$REGISTRY_PATH")

for ((i=0; i<num_patches; i++)); do
  patch=$(jq -c ".patches[$i]" "$REGISTRY_PATH")
  id=$(echo "$patch" | jq -r '.id')
  name=$(echo "$patch" | jq -r '.name')
  patchFile=$(echo "$patch" | jq -r '.patchFile')
  issue=$(echo "$patch" | jq -r '.issue')
  fix=$(echo "$patch" | jq -r '.fix')
  risk=$(echo "$patch" | jq -r '.risk')
  excludes=$(echo "$patch" | jq -r '.excludes | join(", ")')
  
  echo "Generating $patchFile..."
  
  # Write patch header
  cat << EOF > "$patchFile"
From: Folia Compatibility Patch
Subject: [PATCH $id] $name: $fix

AE PATCH REASON: $issue
AE PATCH FIX: $fix
AE PATCH RISK: $risk
AE PATCH EXCLUDES: $excludes

---
EOF

  num_files=$(echo "$patch" | jq '.files | length')
  for ((j=0; j<num_files; j++)); do
    classPath=$(echo "$patch" | jq -r ".files[$j]")
    decompiledFile="src-decompiled/$classPath"
    patchedFile="src-patched/main/java/$classPath"
    
    if [ ! -f "$decompiledFile" ]; then
      echo "Warning: Decompiled file missing: $decompiledFile" >&2
      continue
    fi
    if [ ! -f "$patchedFile" ]; then
      echo "Warning: Patched file missing: $patchedFile" >&2
      continue
    fi
    
    # Run git diff --no-index.
    # Note: git diff --no-index returns exit status 1 if differences are found, so we must allow it.
    set +e
    stdout=$(git diff --no-index -- "$decompiledFile" "$patchedFile")
    exit_code=$?
    set -e
    
    if [ -z "$stdout" ]; then
      echo "No differences found for $classPath"
      continue
    fi
    
    # Process lines to normalize paths to git standard
    cleaned_diff=$(echo "$stdout" | sed \
      -e "s|^--- a/src-decompiled/|--- a/|g" \
      -e "s|^+++ b/src-patched/main/java/|+++ b/|g" \
      -e "s|^diff --git a/src-decompiled/[^ ]* b/src-patched/main/java/[^ ]*|diff --git a/${classPath} b/${classPath}|g" \
      -e "s|^diff --git a/[^ ]* b/src-patched/main/java/[^ ]*|diff --git a/${classPath} b/${classPath}|g" \
      -e "s|^diff --git [^ ]* [^ ]*|diff --git a/${classPath} b/${classPath}|g")
    
    echo "$cleaned_diff" >> "$patchFile"
    echo "" >> "$patchFile"
  done
done

echo "Patch generation complete."
