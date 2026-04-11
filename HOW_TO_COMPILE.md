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
├── gradlew / gradlew.bat                       # Gradle wrapper
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── build.gradle.kts                            # Build config
├── settings.gradle.kts                         # Project name
├── deploy.sh                                   # Simple copy ke server
├── HOW_TO_COMPILE.md                           # This file
├── HOW_TO_MAINTENANCE.md                       # Detailed maintenance guide
├── MappingEnchant.md                           # Enchantment mapping reference
│
├── src-patched/main/                           # Patched Java sources
│   ├── java/net/advancedplugins/ae/
│   │   ├── Core.java                           # Scheduler calls in onEnable/onDisable
│   │   ├── handlers/netsharing/MarketInventory.java
│   │   ├── globallisteners/listeners/ReloadEvent.java
│   │   ├── impl/utils/plugin/UpdateChecker.java
│   │   ├── impl/effects/effects/effects/internal/
│   │   │   ├── ApplyPotionEffect.java
│   │   │   ├── BoostEffect.java
│   │   │   ├── GuardEffect.java
│   │   │   └── TeleportBehindEffect.java
│   │   ├── impl/effects/effects/actions/execution/
│   │   │   └── ExecutionTask.java
│   │   ├── impl/effects/effects/mechanics/triggers/internal/
│   │   │   ├── RepeatingTrigger.java
│   │   │   ├── RepeatingRunnable.java
│   │   │   └── UserRepeaters.java
│   │   └── features/tinkerer/
│   │       └── TinkererInventory.java
│   └── resources/
│       ├── _extracted/                         # Extracted resources from original JAR
│       └── armorSets/                          # Override armor set configs (8 original + 2 custom)
│
├── resources/armorSets/                        # Custom armor set configs (10 tiers)
│   ├── Darkness.yml            (prot:50)
│   ├── Supreme.yml             (prot:45)
│   ├── DimensionalTraveller.yml (prot:40)
│   ├── Koth.yml                (prot:35)
│   ├── Yijki.yml               (prot:30)
│   ├── Phantom.yml             (prot:25)
│   ├── Ranger.yml              (prot:20)
│   ├── Yeti.yml                (prot:15)
│   ├── Mystic.yml              (prot:10)  ← custom
│   └── Novice.yml              (prot:5)   ← custom
│
├── stubs-patched/                              # Stub classes for missing dependencies
│   └── net/kyori/adventure/text/object/
│       └── PlayerHeadObjectContents.java
│
├── libs/                                       # ALL JAR files
│   ├── AdvancedEnchantments-9.22.7.jar          ← Original plugin (source)
│   ├── canvas-api.jar
│   ├── canvas-server.jar
│   ├── bungeecord-chat.jar
│   ├── adventure-*.jar
│   ├── Vault.jar
│   ├── placeholderapi.jar
│   └── ...
│
├── build/                                      # Build output
│   ├── libs/
│   │   └── AdvancedEnchantments-9.22.7-folia-patched.jar
│   ├── classes/java/main/                      # Compiled patched classes
│   ├── classes/stubs/                          # Compiled stubs
│   └── extracted/                              # Extracted original JAR
│
├── vineflower-1.11.2.jar                       # Decompiler for patching new classes
└── decompiled/                                 # Full Vineflower decompilation (reference)
```

---

## What Gets Patched

### Core Scheduler Fixes
| File | What It Fixes |
|------|---------------|
| `Core.java` | `onEnable`/`onDisable` — uses `GlobalRegionScheduler` + `AsyncScheduler` |
| `MarketInventory.java` | Market cache — uses `AsyncScheduler.runAtFixedRate` |
| `ReloadEvent.java` | Reload tick — uses `GlobalRegionScheduler.runDelayed` |
| `UpdateChecker.java` | Update check — no scheduler needed |
| `ExecutionTask.java` | `BukkitRunnable` → `SchedulerUtils.runTaskLater` |
| `BoostEffect.java` | `Bukkit.getScheduler()` → `FoliaScheduler.runTaskLater` |
| `TeleportBehindEffect.java` | `teleport()` → `SchedulerUtils.runTask(() -> teleportAsync)` |
| `RepeatingTrigger.java` | `BukkitTask` → `FoliaScheduler.Task` |
| `TinkererInventory.java` | `Bukkit.getScheduler()` → `FoliaScheduler.runTaskLater` |

### Resource Overrides
| Folder | What It Does |
|--------|-------------|
| `resources/armorSets/` | Custom armor set configs (10 tiers, full enchants, fixed levels) |
| `src-patched/main/resources/_extracted/` | Original resources that get extracted from JAR |

---

## Gradle Commands

| Command | What It Does |
|---------|-------------|
| `./gradlew build` | Build patched JAR |
| `./gradlew deploy` | Build + deploy to server |
| `./gradlew clean build` | Clean rebuild |
| `./gradlew clean build deploy` | One-command rebuild + deploy |
| `./gradlew compileJava` | Only compile, don't package |
| `./gradlew dependencies` | Show dependency tree |

---

## Build Process

```
1. Compile stubs (stubs-patched/) → build/classes/stubs/
2. Compile patched sources (src-patched/) → build/classes/java/main/
3. Extract original JAR (libs/...) → build/extracted/
4. Copy custom resources:
   - resources/armorSets/*.yml → build/extracted/armorSets/ (OVERWRITES original)
   - src-patched/main/resources/_extracted/* → build/extracted/
5. Exclude original classes that have patched versions
6. Overlay patched classes on extracted contents
7. Repack → build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar
8. (deploy) Copy to server plugins directory
```

### How Resource Override Works

`build.gradle.kts` → `copyCustomResources` task:
```kotlin
val copyCustomResources = tasks.register<Copy>("copyCustomResources") {
    dependsOn(extractOriginalJar)

    // ArmorSets override (overwrite original armorSets in JAR)
    val armorSetsDir = file("resources/armorSets")
    if (armorSetsDir.exists()) {
        from(armorSetsDir)
        into(file("${extractDir.get()}/armorSets"))
    }
}
```

This ensures only files in `resources/armorSets/` end up in the JAR's `armorSets/` folder — no anomalous files.

### Stub Classes

`stubs-patched/net/kyori/adventure/text/object/PlayerHeadObjectContents.java` — stub for Paper 1.21.11 Adventure API. Only needed at compile time.

---

## How to Add a New Patched Class

### 1. Identify the broken class from logs
```
java.lang.UnsupportedOperationException
    at AdvancedEnchantments.jar//net.advancedplugins.ae.path.To.Class.method(Class.java:42)
```

### 2. Decompile the class
```bash
mkdir -p /tmp/decompile && cd /tmp/decompile
jar xf /root/github/ae/libs/AdvancedEnchantments-9.22.7.jar net/advancedplugins/ae/path/to/Class.class
java -jar /root/github/ae/vineflower-1.11.2.jar . decompiled 2>&1 | tail -2
cat decompiled/net/advancedplugins/ae/path/to/Class.java
```

### 3. Create patched source
```bash
mkdir -p /root/github/ae/src-patched/main/java/net/advancedplugins/ae/path/to/
# Copy decompiled code, fix scheduler calls, save as Class.java
```

### 4. Exclude original class in build.gradle.kts
```kotlin
exclude("net/advancedplugins/ae/path/to/Class.class")
exclude("net/advancedplugins/ae/path/to/Class\$*.class")  // inner classes
```

### 5. Build and deploy
```bash
./gradlew clean build deploy
```

---

## Verify Patched JAR

```bash
# Extract to temp
cd /tmp && rm -rf verify && mkdir verify && cd verify
unzip -qo /root/github/ae/build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar

# Check armor sets — should only have your custom files
ls armorSets/*.yml
# Expected: Darkness.yml, Supreme.yml, DimensionalTraveller.yml, Koth.yml,
#           Yijki.yml, Phantom.yml, Ranger.yml, Yeti.yml, Mystic.yml, Novice.yml

# Check Core — should show getGlobalRegionScheduler, NOT getScheduler
javap -c net/advancedplugins/ae/Core.class | grep "getScheduler"
# Expected: EMPTY (no results)

# Check ExecutionTask — should show SchedulerUtils, NOT BukkitRunnable
javap -c net/advancedplugins/ae/impl/effects/effects/actions/execution/ExecutionTask.class | grep "BukkitRunnable"
# Expected: EMPTY (no results)
```

---

## Deploy

```bash
# Option 1: Gradle (recommended)
./gradlew deploy

# Option 2: Manual
cp build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar \
   /var/lib/pterodactyl/volumes/876535db-74d8-415c-bc52-21157613398a/plugins/AdvancedEnchantments-9.22.7.jar
```

Then restart the Minecraft server.

---

## Troubleshooting

### `UnsupportedOperationException` from CraftScheduler
A class still uses `Bukkit.getScheduler()` or `BukkitRunnable`. Find the class in logs:
```bash
grep "at AdvancedEnchantments" /var/lib/pterodactyl/volumes/.../logs/latest.log | head -5
```
Patch it following "How to Add a New Patched Class" steps.

### `Must use teleportAsync while in region threading`
Entity.teleport() called on wrong thread. Patch the effect class to use `SchedulerUtils.runTask()` or `teleportAsync()`.

### Armor sets not loading / NPE on startup
Only 8-10 `.yml` files should be in `armorSets/` folder inside JAR. If there are anomalous files (plugin.yml, locale/, etc.), the plugin fails to parse. Check:
```bash
jar tf build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar | grep "^armorSets/"
```
Only `.yml` armor set files should appear.

### `cannot find symbol` when compiling
- Missing import — check decompiled source for imports
- Missing inner class — add `exclude("Class\$*.class")` and include inner class as separate file
- Missing dependency — add JAR to `libs/` and `build.gradle.kts` dependencies

### `zip END header not found`
Corrupt JAR in `libs/`. Remove it:
```bash
file libs/*.jar   # Find corrupt files
rm libs/<corrupt>.jar
./gradlew clean build
```

### Build fails with duplicate class error
Original class not excluded. Add exclude in `build.gradle.kts`:
```kotlin
exclude("net/advancedplugins/ae/path/to/Class.class")
exclude("net/advancedplugins/ae/path/to/Class\$*.class")
```

### `cannot access PlayerHeadObjectContents`
Stub missing. Check:
```bash
ls stubs-patched/net/kyori/adventure/text/object/
```
