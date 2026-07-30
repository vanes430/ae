# Patch Scripts (3)

| Script | Mode | Action |
|--------|------|--------|
| `decompile_src.sh` | — | Extract + decompile AE JAR → `src-decompiled/` |
| `apply_patches.sh` | — | Copy `src-decompiled/`→ `src-patched/`, apply all patches |
| `generate_patches.sh` | no args | Bulk: regenerate ALL patches from diff, preserves headers |
| | `<path>` | Single: diff one file, print patch to stdout |
| `inject-folia-support.sh` | `<plugin.yml>` | Inject `folia-supported: true` after `api-version:` (called by build.gradle.kts) |

All scripts use `set -euo pipefail`, paths resolve from `$SCRIPT_DIR`.
