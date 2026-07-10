# AdvancedEnchantments — Folia Patching Guide

Patches **AdvancedEnchantments v9.23.6** for Folia multi-threaded region compatibility.

---

## Build System

```bash
./gradlew clean build   # Compile patched JAR
./gradlew deploy        # Copy JAR to Pterodactyl server
```

**Output:** `build/libs/AdvancedEnchantments-9.23.6-folia-patched.jar`

### Build Pipeline

1. **compileStubs** — Compiles `stubs-patched/` for type references
2. **compileJava** — Compiles `src-patched/main/java/`
3. **extractOriginalJar** — Unpacks `libs/AdvancedEnchantments-9.23.6.jar`
4. **copyCustomResources** — Overrides `armorSets/`, `customWeapons/`, `enchantments.yml`
5. **jar** — 3-layer assembly:
   - Layer 1: Original JAR (excluding patched classes from `patch-registry.json`)
   - Layer 2: Compiled patched classes
   - Layer 3: Resource overrides

### Dependencies

| JAR | Purpose |
|-----|---------|
| `canvas-api.jar` | Paper/Bukkit API (contains `org.bukkit`, `io.papermc`) |
| `bungeecord-chat.jar` | BaseComponent for FancyMessage/MarketInventory |
| `AdvancedEnchantments-9.23.6.jar` | Original plugin (reference) |
| `placeholderapi.jar` | PlaceholderAPI hook |
| `Vault.jar` | Vault economy hook |

---

## Patch System

26 patches in `patches/`, indexed by `patch-registry.json`.

### Patch Workflow

```bash
# Decompile original JAR → src-decompiled/
bash tools/scripts/decompile.sh

# Apply patches to src-decompiled/ → src-patched/
bash tools/scripts/apply-patches.sh

# Regenerate patches from diff
bash tools/scripts/generate-patches.sh

# Verify patches apply cleanly
bash tools/scripts/verify-patches.sh
```

Vineflower decompiler is auto-downloaded from GitHub on first run.

### Patch Format

Each `.patch` file contains:
```
AE PATCH REASON: <why needed>
AE PATCH FIX: <what it does>
AE PATCH RISK: LOW | MEDIUM | HIGH
AE PATCH EXCLUDES: <classes to skip>
---
diff --git a/... b/...
```

### Risk Levels

| Risk | Count | Scope |
|------|-------|-------|
| LOW | 8 | Scheduler migration, FancyMessage |
| MEDIUM | 9 | Effects, armor, teleport |
| HIGH | 9 | Inventory, weapons, triggers |

---

## Folia Scheduling Rules

### Entity/Player Tasks
```java
entity.getScheduler().runDelayed(plugin, task -> {
    if (entity.isValid()) {
        entity.setHealth(newHealth);
    }
}, null, delayTicks);
```

### Repeating Tasks
```java
ScheduledTask task = entity.getScheduler().runAtFixedRate(
    plugin, taskInstance -> { /* ... */ },
    null, initialDelayTicks, periodTicks
);
```

### Block/Location Tasks
```java
Bukkit.getRegionScheduler().execute(plugin, location, () -> {
    location.getBlock().setType(material);
});
```

### Async/Global Tasks
```java
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
    // Database, HTTP, etc.
}, initialDelay, period, TimeUnit.MILLISECONDS);
```

---

## Thread-Safety Rules

1. No static raw `HashMap` for player data — use `ConcurrentHashMap`
2. Use `computeIfAbsent` / `putIfAbsent` for atomic writes
3. Wrap synchronized map iteration in `synchronized` block
4. Use `Collections.synchronizedList(new ArrayList<>())` for shared lists

---

## Upgrading to Newer Version

1. Replace `libs/AdvancedEnchantments-NEW.jar`
2. Update `originalJar` in `build.gradle.kts`
3. Run `bash tools/scripts/decompile.sh`
4. Run `bash tools/scripts/apply-patches.sh` — resolve conflicts
5. Run `bash tools/scripts/generate-patches.sh`
6. Run `./gradlew clean build` — verify compilation
