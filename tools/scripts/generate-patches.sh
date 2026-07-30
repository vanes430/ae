#!/bin/bash
set -euo pipefail

if [ ! -d "src-decompiled" ] || [ ! -d "src-patched" ]; then
  echo "Error: Need both src-decompiled/ and src-patched/" >&2
  exit 1
fi

# Find differing files
diffs=()
while IFS= read -r line; do
  f=${line#src-decompiled/}
  diffs+=("$f")
done < <(diff -rq src-decompiled src-patched 2>/dev/null | grep "differ$" | sed 's/Files src-decompiled\///;s/ and.*//')

if [ ${#diffs[@]} -eq 0 ]; then
  echo "No differences found."
  exit 0
fi

echo "Found ${#diffs[@]} differing files"

next_num=$(ls patches/*.patch 2>/dev/null | wc -l)
next_num=$((next_num + 1))

for f in "${diffs[@]}"; do
  diff -u "src-decompiled/$f" "src-patched/$f" 2>/dev/null | tail -n +3 > "patches/.tmp-diff" || true

  # Check if patch already exists
  existing=$(grep -rl "b/$f" patches/*.patch 2>/dev/null || true)

  if [ -n "$existing" ]; then
    # Extract existing AE metadata lines, preserve them
    ae_meta=$(grep "^AE PATCH" "$existing" || true)
    echo "  UPDATE: $(basename "$existing")"

    # Extract subject line
    subj=$(grep "^Subject:" "$existing" | head -1 || echo "")

    # Write header + old AE metadata + new diff
    {
      head -1 "$existing"  # From:
      echo "$subj"
      echo ""
      [ -n "$ae_meta" ] && echo "$ae_meta" || echo -e "AE PATCH REASON: \nAE PATCH FIX: \nAE PATCH RISK: "
      echo ""
      echo "---"
      echo "diff --git a/${f} b/${f}"
      echo "--- a/${f}"
      echo "+++ b/${f}"
      cat patches/.tmp-diff
    } > "$existing"
  else
    name=$(basename "$f" .java)
    pnum=$(printf "%04d" $next_num)
    pfile="patches/${pnum}-${name}.patch"

    echo "  GEN: $pfile"

    cat > "$pfile" << EOF
From: Folia Compatibility Patch
Subject: [PATCH ${pnum}] ${name}: 

AE PATCH REASON: 
AE PATCH FIX: 
AE PATCH RISK: 

---
diff --git a/${f} b/${f}
--- a/${f}
+++ b/${f}
$(cat patches/.tmp-diff)
EOF

    next_num=$((next_num + 1))
  fi

  rm -f patches/.tmp-diff
done
