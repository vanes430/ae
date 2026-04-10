# HOW TO COMPILE — AdvancedEnchantments Folia Patch

## Quick Start

```bash
# Build + deploy ke server
./gradlew deploy

# Atau terpisah
./gradlew build        # Build patched JAR
./gradlew clean build  # Clean rebuild
```

Output: `build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar`

---

## Directory Structure

```
ae/
├── gradlew / gradlew.bat               # Gradle wrapper
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── build.gradle.kts                    # Build config (single-project)
├── settings.gradle.kts                 # Project name
├── deploy.sh                           # Simple copy ke server
├── HOW_TO_COMPILE.md                   # This file
├── HOW_TO_MAINTENANCE.md               # Detailed maintenance guide
│
├── src-patched/                        # Patched Java sources
│   └── net/advancedplugins/ae/
│       ├── Core.java                   # Scheduler calls in onEnable/onDisable
│       ├── handlers/netsharing/MarketInventory.java
│       ├── globallisteners/listeners/ReloadEvent.java
│       ├── impl/utils/plugin/UpdateChecker.java
│       └── impl/effects/effects/effects/internal/
│           ├── ApplyPotionEffect.java
│           └── GuardEffect.java
│
├── stubs-patched/                      # Stub classes for missing dependencies
│   └── net/kyori/adventure/text/object/
│       └── PlayerHeadObjectContents.java
│
├── libs/                               # ALL JAR files
│   ├── AdvancedEnchantments-9.22.7.jar  ← Original plugin (source)
│   ├── canvas-api.jar
│   ├── Vault.jar
│   ├── placeholderapi.jar
│   └── ...
│
├── build/                              # Build output
│   ├── libs/
│   │   └── AdvancedEnchantments-9.22.7-folia-patched.jar
│   ├── classes/java/main/              # Compiled patched classes
│   ├── classes/stubs/                  # Compiled stubs
│   └── extracted/                      # Extracted original JAR
│
├── bytecode-patcher/                   # Legacy Javassist patchers (optional)
└── decompiled/                         # Full Vineflower decompilation (reference)
```

---

## What Gets Patched

| File | What It Fixes |
|------|---------------|
| `Core.java` | `onEnable`/`onDisable` — uses `GlobalRegionScheduler` + `AsyncScheduler` |
| `MarketInventory.java` | Market cache — uses `AsyncScheduler.runAtFixedRate` |
| `ReloadEvent.java` | Reload tick — uses `GlobalRegionScheduler.runDelayed` |
| `UpdateChecker.java` | Update check — no scheduler needed |
| `ApplyPotionEffect.java` | Potion effect — uses `EntityScheduler.execute` |
| `GuardEffect.java` | Guard entity — Folia-compatible scheduling |

---

## Gradle Commands

| Command | What It Does |
|---------|-------------|
| `./gradlew build` | Build patched JAR |
| `./gradlew deploy` | Build + deploy to server |
| `./gradlew clean` | Clean build output |
| `./gradlew compileJava` | Only compile, don't package |
| `./gradlew dependencies` | Show dependency tree |

---

## Build Process

```
1. Compile stubs (stubs-patched/) → build/classes/stubs/
2. Compile patched sources (src-patched/) → build/classes/java/main/
3. Extract original JAR (libs/...) → build/extracted/
4. Overlay patched classes on extracted contents
5. Repack → build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar
6. (deploy) Copy to server plugins directory
```

### Stub Classes

`stubs-patched/net/kyori/adventure/text/object/PlayerHeadObjectContents.java` — stub for Paper 1.21.11 Adventure API. Only needed at compile time.

---

## Verify Patched JAR

```bash
cd /tmp && rm -rf verify && mkdir verify && cd verify
unzip -qo build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar
javap -c net/advancedplugins/ae/handlers/netsharing/MarketInventory.class | grep "getAsyncScheduler"
```

**Expected:** `getAsyncScheduler` ✅ — no `runTaskTimerAsynchronously` ✅

---

## Deploy

```bash
# Option 1: Gradle (recommended)
./gradlew deploy

# Option 2: bash script
bash deploy.sh

# Option 3: Manual
cp build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar \
   /var/lib/pterodactyl/volumes/.../plugins/AdvancedEnchantments-9.22.7.jar
```

Then restart the Minecraft server.

---

## Troubleshooting

### `UnsupportedOperationException` from CraftScheduler
JAR outdated. Rebuild:
```bash
./gradlew clean deploy
```

### `cannot access PlayerHeadObjectContents`
Stub missing. Check:
```bash
ls stubs-patched/net/kyori/adventure/text/object/
```

### `zip END header not found`
A corrupt JAR in `libs/`. Remove it:
```bash
file libs/*.jar   # Find corrupt files
rm libs/<corrupt>.jar
./gradlew clean build
```

### Add a new patched file
1. Add source in `src-patched/<package-path>.java`
2. If compilation fails → add stub to `stubs-patched/` or exclude in `build.gradle.kts`
3. `./gradlew build`
