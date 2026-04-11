# AdvancedEnchantments — Maintenance Guide

## Overview

This project maintains a **Folia/Canvas-compatible patched jar** of **AdvancedEnchantments v9.22.7**.
The original plugin uses Bukkit scheduler APIs incompatible with Folia's regionized threading model.

---

## Build System

### Gradle (Primary)
```bash
./gradlew clean build        # Build patched JAR
./gradlew deploy             # Build + deploy to server
./gradlew clean build deploy # One-command rebuild + deploy
```
Output: `build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar`

### How It Works
1. Compile stubs → `build/classes/stubs/`
2. Compile patched sources → `build/classes/java/main/`
3. Extract original JAR → `build/extracted/`
4. Override resources (armorSets, customWeapons)
5. Overlay patched classes on extracted contents
6. Repack → patched JAR

---

## Patched Classes (Folia Compatibility)

### Scheduler-Related Patches

| File | Original | Fix |
|------|----------|-----|
| **Core.java** | `Bukkit.getScheduler().runTaskLater/cancelTasks` | `Bukkit.getGlobalRegionScheduler()`, `Bukkit.getAsyncScheduler()` |
| **ExecutionTask.java** | `new BukkitRunnable().runTaskLater()` | `SchedulerUtils.runTaskLater()` |
| **RepeatingTrigger.java** | `BukkitTask`, `BukkitRunnable.runTaskTimer()` | `FoliaScheduler.Task`, `FoliaScheduler.runTaskTimer()` |
| **RepeatingRunnable.java** | extends `BukkitRunnable` | implements `Runnable` |
| **UserRepeaters.java** | `HashMap<EquipmentSlot, List<BukkitTask>>` | `HashMap<EquipmentSlot, List<FoliaScheduler.Task>>` |
| **TeleportBehindEffect.java** | `target.teleport()` on wrong thread | `SchedulerUtils.runTask(() -> teleport)` |
| **BoostEffect.java** | `Bukkit.getScheduler().runTaskLater()` | `FoliaScheduler.runTaskLater()` |
| **TinkererInventory.java** | `Bukkit.getScheduler().runTaskLater()` | `FoliaScheduler.runTaskLater()` |

### Resource Override
| File | Purpose |
|------|---------|
| **ApplyPotionEffect.java** | Folia-compatible scheduler for potion effects |
| **GuardEffect.java** | Folia-compatible scheduling for guard entity |
| **MarketInventory.java** | `Bukkit.getAsyncScheduler().runAtFixedRate()` |
| **ReloadEvent.java** | `Bukkit.getGlobalRegionScheduler().runDelayed()` |
| **UpdateChecker.java** | No scheduler needed (removed) |
| **FancyMessage.java** | `ComponentSerializer.parse()` instead of `Bukkit.dispatchCommand` |

### Armor Sets (10 Tiers)
Custom armor set configs with full enchant coverage:

| Tier | Armor Set | Prot/Unbrk | Enchant Count |
|------|-----------|------------|---------------|
| 1 | Darkness | 50 | Full max level |
| 2 | Supreme | 45 | All enchants high |
| 3 | DimensionalTraveller | 40 | All enchants mod-high |
| 4 | KOTH | 35 | Moderate enchants |
| 5 | Yijki | 30 | Low-moderate |
| 6 | Phantom | 25 | Basic enchants |
| 7 | Ranger | 20 | Few enchants |
| 8 | Yeti | 15 | Minimal |
| 9 | Mystic | 10 | Bare minimum |
| 10 | Novice | 5 | Tank + Spirits only |

---

## How to Add a New Patched Class

### Step 1: Identify the broken class
```
java.lang.UnsupportedOperationException
    at org.bukkit.craftbukkit.scheduler.CraftScheduler.handle
    at AdvancedEnchantments.jar//net.advancedplugins.ae.path.To.BrokenClass.method(BrokenClass.java:42)
```

### Step 2: Decompile the class
```bash
mkdir -p /tmp/decompile
cd /tmp/decompile
jar xf /root/github/ae/libs/AdvancedEnchantments-9.22.7.jar net/advancedplugins/ae/path/to/BrokenClass.class
java -jar /root/github/ae/vineflower-1.11.2.jar . decompiled 2>&1 | tail -2
cat decompiled/net/advancedplugins/ae/path/to/BrokenClass.java
```

### Step 3: Create patched source
```bash
# Create directory structure matching package path
mkdir -p /root/github/ae/src-patched/main/java/net/advancedplugins/ae/path/to/
# Copy decompiled source, edit with Folia-compatible code
```

### Step 4: Update build.gradle.kts
Add exclude for the original class:
```kotlin
exclude("net/advancedplugins/ae/path/to/BrokenClass.class")
exclude("net/advancedplugins/ae/path/to/BrokenClass\$*.class")  // inner classes
```

### Step 5: Build and deploy
```bash
./gradlew clean build deploy
```

---

## Common Folia Fixes

### Bukkit.getScheduler() → FoliaScheduler
```java
// Before (BROKEN)
Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);

// After (Folia-compatible)
FoliaScheduler.runTaskLater(plugin, runnable, delay);
FoliaScheduler.runTaskTimer(plugin, runnable, delay, period);
```

### BukkitRunnable → SchedulerUtils
```java
// Before (BROKEN)
new BukkitRunnable() {
    public void run() { ... }
}.runTaskLater(plugin, delay);

// After (Folia-compatible)
SchedulerUtils.runTaskLater(() -> { ... }, delay);
```

### teleport() → teleportAsync (inside scheduler task)
```java
// Before (BROKEN on Folia)
target.teleport(location);

// After (Folia-compatible)
SchedulerUtils.runTask(() -> {
    if (FoliaScheduler.isFolia()) {
        target.teleportAsync(location).join();
    } else {
        target.teleport(location);
    }
});
```

### BukkitTask → FoliaScheduler.Task
```java
// Before (BROKEN)
BukkitTask task = new MyRunnable().runTaskTimer(plugin, 0, 20);
task.cancel();

// After (Folia-compatible)
FoliaScheduler.Task task = FoliaScheduler.runTaskTimer(plugin, runnable, 0, 20);
task.cancel();
```

---

## Directory Structure

```
ae/
├── build.gradle.kts                          # Build configuration
├── gradlew / gradlew.bat                     # Gradle wrapper
│
├── src-patched/                              # Patched Java sources
│   └── main/
│       ├── java/net/advancedplugins/ae/
│       │   ├── Core.java                     # Scheduler calls in onEnable/onDisable
│       │   ├── handlers/netsharing/MarketInventory.java
│       │   ├── globallisteners/listeners/ReloadEvent.java
│       │   ├── impl/utils/plugin/UpdateChecker.java
│       │   ├── impl/effects/effects/effects/internal/
│       │   │   ├── ApplyPotionEffect.java
│       │   │   ├── BoostEffect.java
│       │   │   ├── GuardEffect.java
│       │   │   └── TeleportBehindEffect.java
│       │   ├── impl/effects/effects/actions/execution/
│       │   │   └── ExecutionTask.java
│       │   ├── impl/effects/effects/mechanics/triggers/internal/
│       │   │   ├── RepeatingTrigger.java
│       │   │   ├── RepeatingRunnable.java
│       │   │   └── UserRepeaters.java
│       │   └── features/tinkerer/
│       │       └── TinkererInventory.java
│       └── resources/
│           ├── _extracted/                   # Extracted resources from original JAR
│           └── armorSets/                    # Override armor set configs
│
├── stubs-patched/                            # Stub classes for missing dependencies
│   └── net/kyori/adventure/text/object/
│       └── PlayerHeadObjectContents.java
│
├── resources/
│   └── armorSets/                            # Custom armor set configs (10 tiers)
│       ├── Darkness.yml
│       ├── Supreme.yml
│       ├── DimensionalTraveller.yml
│       ├── Koth.yml
│       ├── Yijki.yml
│       ├── Phantom.yml
│       ├── Ranger.yml
│       ├── Yeti.yml
│       ├── Mystic.yml
│       └── Novice.yml
│
├── libs/                                     # ALL dependency JARs
│   ├── AdvancedEnchantments-9.22.7.jar       # Original plugin (source)
│   ├── canvas-api.jar
│   ├── canvas-server.jar
│   ├── bungeecord-chat.jar
│   ├── adventure-*.jar
│   ├── Vault.jar
│   └── placeholderapi.jar
│
├── build/                                    # Build output
│   ├── libs/
│   │   └── AdvancedEnchantments-9.22.7-folia-patched.jar
│   └── extracted/                            # Extracted original JAR
│
├── vineflower-1.11.2.jar                     # Decompiler
├── deploy.sh                                 # Simple copy deploy script
├── HOW_TO_COMPILE.md                         # Build commands reference
└── HOW_TO_MAINTENANCE.md                     # This file
```

---

## Scheduler Utilities

### FoliaScheduler (from original JAR)
```java
public class FoliaScheduler {
    public static final boolean isFolia = true;  // auto-detected

    // Run immediately
    public static Task runTask(Plugin plugin, Runnable runnable);

    // Run after delay (in ticks)
    public static Task runTaskLater(Plugin plugin, Runnable runnable, long delayTicks);

    // Run repeatedly (in ticks)
    public static Task runTaskTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks);

    // Async variants
    public static Task runTaskAsynchronously(Plugin plugin, Runnable runnable);
    public static Task runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delayTicks);
    public static Task runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks);

    // Cancel all tasks
    public static void cancelAll(Plugin plugin);
}
```

### SchedulerUtils (from original JAR)
Wrapper around FoliaScheduler for simpler usage:
```java
public class SchedulerUtils {
    public static int runTaskLater(Runnable task, long delay);  // default plugin: ASManager.getInstance()
    public static int runTaskLater(Runnable task);              // default delay: 1 tick
    public static int runTaskTimer(Runnable task, long initialDelay, long period);
    public static int runTaskTimerAsync(Runnable task, long initialDelay, long period);
    public static int runTask(Runnable task);
    public static int runTaskAsync(Runnable task);
}
```

---

## Verify Patched JAR

```bash
cd /tmp && rm -rf verify && mkdir verify && cd verify
unzip -qo /root/github/ae/build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar

# Check Core — should show getGlobalRegionScheduler, NOT getScheduler
javap -c net/advancedplugins/ae/Core.class | grep "getScheduler"
# Expected: EMPTY (no results for getScheduler)

# Check ExecutionTask — should show SchedulerUtils, NOT BukkitRunnable
javap -c net/advancedplugins/ae/impl/effects/effects/actions/execution/ExecutionTask.class | grep "BukkitRunnable\|runTaskLater"
# Expected: SchedulerUtils.runTaskLater

# Check armor sets — should only have 8-10 yml files
ls armorSets/*.yml | wc -l
```

---

## Troubleshooting

### `UnsupportedOperationException` from CraftScheduler
A class still uses `Bukkit.getScheduler()` or `BukkitRunnable`. Find the class in logs:
```bash
# Search for the broken class in logs
grep "at AdvancedEnchantments" logs/latest.log | grep -v "SchedulerUtils\|FoliaScheduler"
```
Then patch it following the "How to Add a New Patched Class" steps above.

### `Must use teleportAsync while in region threading`
Entity.teleport() called outside main thread. Fix:
```java
SchedulerUtils.runTask(() -> target.teleportAsync(location).join());
```

### `cannot find symbol: class FoliaScheduler.Task`
Make sure `FoliaScheduler` import is present:
```java
import net.advancedplugins.ae.impl.utils.FoliaScheduler;
```

### Build fails with duplicate class error
Add exclude for the original class in `build.gradle.kts`:
```kotlin
exclude("net/advancedplugins/ae/path/to/Class.class")
exclude("net/advancedplugins/ae/path/to/Class\$*.class")
```

### Armor sets not loading
Check `resources/armorSets/` folder and `build.gradle.kts` `copyCustomResources` task. Only `.yml` files in this folder are included in the JAR.

### New enchant fails on Folia
Check which effect class is involved. Most effects use `ExecutionTask.activate()` which already uses `SchedulerUtils`. If a specific effect class uses `Bukkit.getScheduler()` directly, patch it to use `FoliaScheduler` or `SchedulerUtils`.

---

## Updating to a New Plugin Version

1. **Download the new original jar** → place as `libs/AdvancedEnchantments-NEW.jar`
2. **Update version in `build.gradle.kts`**:
   ```kotlin
   val originalJar = file("libs/AdvancedEnchantments-NEW.jar")
   version = "NEW-folia"
   ```
3. **Decompile the new jar** and compare patched classes
4. **Update patched sources** if the original code changed
5. **Run** `./gradlew clean build deploy`
6. **Test on server** and check logs for new errors

---

## Quick Reference: All Patched Classes

| # | Class | Issue | Fix |
|---|-------|-------|-----|
| 1 | Core.java | Bukkit scheduler | GlobalRegionScheduler + AsyncScheduler |
| 2 | MarketInventory.java | Bukkit scheduler | AsyncScheduler |
| 3 | ReloadEvent.java | Bukkit scheduler | GlobalRegionScheduler |
| 4 | UpdateChecker.java | Bukkit scheduler | Removed scheduler |
| 5 | ApplyPotionEffect.java | Bukkit scheduler | SchedulerUtils |
| 6 | GuardEffect.java | Bukkit scheduler | FoliaScheduler |
| 7 | ExecutionTask.java | BukkitRunnable | SchedulerUtils |
| 8 | TeleportBehindEffect.java | teleport() sync | SchedulerUtils.runTask |
| 9 | BoostEffect.java | Bukkit.getScheduler() | FoliaScheduler |
| 10 | RepeatingTrigger.java | BukkitTask | FoliaScheduler.Task |
| 11 | RepeatingRunnable.java | BukkitRunnable | Runnable |
| 12 | UserRepeaters.java | BukkitTask | FoliaScheduler.Task |
| 13 | TinkererInventory.java | Bukkit.getScheduler() | FoliaScheduler |
| 14 | FancyMessage.java | dispatchCommand async | ComponentSerializer |
| 15 | Armor Sets (10) | Config only | resources/armorSets/ |
