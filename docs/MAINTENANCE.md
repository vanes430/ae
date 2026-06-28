# AdvancedEnchantments — Maintenance & Porting Guide

This project maintains a **Folia-compatible patched build** of **AdvancedEnchantments v9.23.6**. 
Because the original plugin was designed for legacy Bukkit (which relies on single-threaded execution and a global synchronous tick), it is incompatible with Folia's multi-threaded, regionized ticker model. This project patches those compatibility issues.

---

## 🛠️ Build & Deploy System

The build system extracts the original JAR, compiles the patched source code, overlays it, and packages the final Folia-ready JAR.

```bash
./gradlew clean build        # Compile and package the patched JAR
./gradlew deploy             # Compile, package, and deploy to testing server
./gradlew clean build deploy # Complete rebuild and deploy sequence
```
**Output Location:** `build/libs/AdvancedEnchantments-9.23.6-folia-patched.jar`

### How the Build Process Works
1. **Compile Stubs:** Compiles dummy stub classes in `stubs-patched/` for compile-only dependencies.
2. **Compile Patched Classes:** Compiles our custom source code located in `src-patched/main/java`.
3. **Extract Original Jar:** Unpacks the raw class files and assets from `libs/AdvancedEnchantments-9.23.6.jar` to `build/extracted`.
4. **Exclude Classes:** The Gradle build reads `patch-registry.json` and excludes the original counterparts of the patched classes from packaging.
5. **Overlay:** Injects our newly compiled classes and resource overrides (like custom armor sets in `resources/`) into the final JAR.

---

## 🚀 Folia Architecture & Scheduling Rules

In Folia, game ticks are split across thread regions. Doing synchronous modifications on players, inventories, or entities from other threads causes state exceptions (e.g. `NullPointerException: Cannot read field "world"`) or server crashes.

We avoid global task wrappers (like `SchedulerUtils` or `FoliaScheduler`) and instead schedule tasks **directly** on the appropriate regionized thread scheduler:

### 1. Entity & Player Tasks
If a task modifies a player's health, inventory, position, or attributes, it **must** run on the entity's scheduler.
```java
// Thread-safe Entity/Player Delayed Task
entity.getScheduler().runDelayed(
    pluginInstance, 
    task -> {
        // Runs on the region thread of the entity
        if (entity.isValid()) {
            entity.setHealth(newHealth);
        }
    }, 
    null, 
    delayTicks
);
```

### 2. Repeating Tasks
Repeating triggers (like repeating enchantments) must run on the entity's thread context:
```java
// Thread-safe Entity Repeating Task
ScheduledTask task = entity.getScheduler().runAtFixedRate(
    pluginInstance,
    taskInstance -> {
        // Repeatedly executed on the entity's region thread
    },
    null,
    initialDelayTicks,
    periodTicks
);
```

### 3. Block & Location-Bound Tasks
Modifying blocks (setting block types, updating states) must be executed on the region owning the location:
```java
// Thread-safe Region-specific Task
Bukkit.getRegionScheduler().execute(
    pluginInstance,
    location,
    () -> {
        location.getBlock().setType(material);
    }
);
```

### 4. Async & Global Tasks
Network requests, database I/O, or global caching must run on Folia's async or global region schedulers:
```java
// Thread-safe Async Repeating Task
Bukkit.getAsyncScheduler().runAtFixedRate(
    pluginInstance,
    task -> {
        // Database check, HTTP fetch, etc.
    },
    initialDelay,
    period,
    TimeUnit.MILLISECONDS
);
```

---

## 🔒 Concurrency & Thread-Safety Rules

Because different player events (like armor equipping or clicks) run concurrently on different region threads, sharing regular `HashMap` or `ArrayList` instances across players will lead to `ConcurrentModificationException` or memory corruption.

### Rules for Shared State:
1. **Never use static raw HashMaps** for tracking player data. Use `ConcurrentHashMap` instead.
2. **Atomic Writes:** Use `map.computeIfAbsent(...)` or `map.putIfAbsent(...)` to prevent race conditions during initialization.
3. **Synchronized Iteration:** If you must copy or iterate a synchronized map's keySet/values, wrap it in a `synchronized` block:
   ```java
   synchronized (sharedMap) {
       keysCopy = new ArrayList<>(sharedMap.keySet());
   }
   ```
4. **Thread-Safe Lists:** Use `Collections.synchronizedList(new ArrayList<>())` for list operations accessed from multiple player threads.

---

## 📂 Patched Files Registry

Patches are structured as **one patch file per Java class**. The mappings are registered in [patch-registry.json](file:///root/ae/patch-registry.json):

| Patch ID | Class Name | Folder / Package | Migration Details |
|---|---|---|---|
| `0001` | `Core` | `net/advancedplugins/ae` | Fixed plugin disable task cancellation via global/async schedulers. |
| `0002` | `TinkererInventory` | `net/advancedplugins/ae/features/tinkerer` | Thread-safe `ConcurrentHashMap` for open inventories and synchronized trade lists. |
| `0003` | `AdvancedWeapon` | `net/advancedplugins/ae/features/weapons` | Read-only enchant ability cache validation. |
| `0004` | `ReloadEvent` | `net/advancedplugins/ae/globallisteners/listeners` | Migrated reload ticks to `Bukkit.getGlobalRegionScheduler()`. |
| `0005` | `FancyMessage` | `net/advancedplugins/ae/utils/fanciful` | Fixed chat package serialization. |
| `0006` | `MarketInventory` | `net/advancedplugins/ae/handlers/netsharing` | Thread-safe async cache mapping and synchronized key pagination. |
| `0007` | `ReallyFastBlockHandler` | `net/advancedplugins/ae/impl/utils` | Removed legacy NMS reflections. Implemented region thread placement. |
| `0008` | `FoliaScheduler` | `net/advancedplugins/ae/impl/utils` | Left intact as a compatibility stub for unpatched classes. |
| `0009` | `ArmorListener` | `net/advancedplugins/ae/impl/effects/armorutils` | Removed legacy debounce task queue; scheduled changes on entity scheduler. |
| `0010` | `UpdateChecker` | `net/advancedplugins/ae/impl/utils/plugin` | Migrated update requests to Folia `AsyncScheduler`. |
| `0011` | `FancyMessage` | `net/advancedplugins/ae/impl/utils/fanciful` | Cleaned duplicate serialization logic. |
| `0012` | `MinecraftVersion` | `net/advancedplugins/ae/impl/utils/nbt/utils` | Removed obsolete `ClassWrapper` dependency. |
| `0013` | `BoostEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Migrated velocity changes to target entity scheduler. |
| `0014` | `ApplyPotionEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Thread-safe permanent potion cache (`ConcurrentHashMap`). |
| `0015` | `GuardEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Thread-safe guard mapping (`ConcurrentHashMap`). |
| `0016` | `ExtinguishEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Scheduled entity fire ticks update on target entity scheduler. |
| `0017` | `TeleportBehindEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Replaced synchronous teleports with `entity.teleportAsync()`. |
| `0018` | `ExecutionTask` | `net/advancedplugins/ae/impl/effects/effects/actions/execution` | Scheduled execution delays on entity scheduler (or region scheduler for locations). |
| `0019` | `UserRepeaters` | `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal` | Replaced legacy tasks container with `ScheduledTask` lists. |
| `0020` | `ArmorWearTrigger` | `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal` | Scheduled player armor changes and health updates on entity scheduler. |
| `0021` | `RepeatingTrigger` | `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal` | Thread-safe tasks tracker and repeating tasks scheduled on entity's ticker. |
| `0022` | `DamageHandler` | `net/advancedplugins/ae/impl/effects/effects/actions/handlers` | Migrated post-respawn delayed teleport to player entity scheduler and `teleportAsync`. |
| `0023` | `TeleportEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Replaced legacy synchronous player/entity teleportation with `teleportAsync`. |
| `0024` | `FireballEffect` | `net/advancedplugins/ae/impl/effects/effects/effects/internal` | Replaced legacy synchronous fireball projectile teleportation with `teleportAsync`. |

---

## 🔍 Upgrading to a Newer Plugin Version

When a new version of `AdvancedEnchantments` is released:
1. **Replace original JAR:** Place the new JAR in `libs/AdvancedEnchantments-NEW.jar`.
2. **Update Gradle build config:** In `build.gradle.kts`, update `originalJar` and target versions to match the new file name.
3. **Decompile raw sources:** Decompile classes in the new JAR to update `src-decompiled` files.
4. **Regenerate differences:** Run `bun tools/scripts/generate-patches.js` to ensure the patch definitions match the new version's base code, resolving any compile-time or patch conflicts.
