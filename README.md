# AdvancedEnchantments — Folia Patched

Folia-compatibility patch layer for AdvancedEnchantments plugin. Decompiles original JAR, applies scheduler migration patches, repackages as fat JAR.

Base: **AdvancedEnchantments 9.24.2**
Patches: **27**

---

## Quick Start (from zero)

```bash
# 1. Clone
git clone <repo> ae
cd ae

# 2. Place AdvancedEnchantments JAR
cp /path/to/AdvancedEnchantments-<VERSION>.jar libs/

# 3. Decompile
bash tools/scripts/decompile_src.sh

# 4. Apply patches
bash tools/scripts/apply_patches.sh

# 5. Build patched JAR
./gradlew clean build
# → build/libs/AdvancedEnchantments-<VERSION>-folia-patched.jar
```

---

## File Security Map

### Git-committed (safe to delete = need restore from git)

| File/Dir | Purpose |
|----------|---------|
| `patches/*.patch` | **27 patch files** — the actual Folia changes. Only source of truth for patching. |
| `resources/` | Override files (`plugin.yml`, `enchantments.yml`, armor sets, weapons) |
| `tools/scripts/` | 3 scripts: decompile_src.sh, apply_patches.sh, generate_patches.sh |
| `build.gradle.kts` | Gradle build — version auto-detection, fat JAR assembly |
| `settings.gradle.kts` | Project name |
| `gradlew` / `gradlew.bat` | Gradle wrapper |
| `docs/` | Documentation |

### Auto-generated (safe to delete, can regenerate)

| File/Dir | Origin | Regenerate Command |
|----------|--------|--------------------|
| `src-decompiled/` | Decompiler output (read-only) | `bash tools/scripts/decompile.sh` |
| `src-patched/` | Copy of decompiled + patches applied | `bash tools/scripts/apply-patches.sh` |
| `build/` | Gradle build output | `./gradlew build` |
| `.gradle/` | Gradle cache | auto |
| `tools/vineflower.jar` | Downloaded decompiler | auto via `decompile.sh` |
| `patches-backup.zip` | Manual backup | re-create via zip |

### Git-ignored (never committed)

| Entry | Reason |
|-------|--------|
| `.gradle/` | Gradle cache |
| `build/` | Build output |
| `bin/` | Eclipse/JDTLS compilation output |
| `src-decompiled/` | Regenerable from JAR |
| `src-patched/` | Regenerable from decompiled + patches |
| `.idea/` | IntelliJ config (local) |
| `.project`, `.classpath`, `.settings/` | Eclipse/JDTLS config (local) |
| `*.log` | Log files |
| `tools/vineflower.jar` | Downloaded tool |

### Hand-edit only here

Only edit files in **`src-patched/`**. Everything else is either auto-generated or committed patches.

**Never** edit files in `src-decompiled/` — it's a read-only reference copy of the original decompiled JAR.

**Never** edit `.patch` files directly — regenerate them after editing `src-patched/`.

---

## Daily Maintenance Cycle

```
src-patched/  →  generate_patches.sh  →  patches/*.patch  →  build
```

1. **Edit** — modify files in `src-patched/` directly
2. **Generate** — `bash tools/scripts/generate_patches.sh`
   - Saves old patch headers automatically
   - Generates fresh diffs against `src-decompiled/`
   - Restores headers or creates blank template
3. **Build** — `./gradlew clean build`
4. **Deploy** — `./gradlew deploy` (optional, copies to plugins dir)

### Fresh start (discard all changes)

```bash
bash tools/scripts/apply_patches.sh   # re-copies src-decompiled → src-patched, re-applies patches
```

---

## Upgrading to New AE Version

```
1. Place AdvancedEnchantments-<NEWVER>.jar into libs/
2. Remove old JAR from libs/  (exactly 1 allowed)
3. bash tools/scripts/decompile_src.sh
4. bash tools/scripts/apply_patches.sh
5. Fix any FAIL-ed patches manually in src-patched/
6. bash tools/scripts/generate_patches.sh
7. Check patch headers — auto-restored from old patches
8. ./gradlew clean build
```

---

## Project Structure

```
ae/
├── build.gradle.kts       # Build (auto-detects version from libs/)
├── libs/                  # Dependencies + AdvancedEnchantments-*.jar
├── patches/               # 27 .patch files (Folia migration diffs)
├── resources/             # Override files (plugin.yml, etc.)
├── src-decompiled/        # Read-only decompiler output
├── src-patched/           # Editable source (decompiled + patches applied)
├── tools/scripts/         # 3 scripts: decompile_src.sh, apply_patches.sh, generate_patches.sh
└── docs/                  # Documentation
```
