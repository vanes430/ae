# AdvancedEnchantments — Maintenance Guide

## Overview

This project maintains a **Folia/Canvas-compatible patched jar** of **AdvancedEnchantments v9.22.7**.  
The original plugin uses APIs incompatible with Folia's regionized threading model.

---

## Problems Patched

### 1. FancyMessage.send() — async dispatchCommand
```
java.lang.IllegalStateException: Dispatching command async
```
Original: `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + ...)`  
Patched: `ComponentSerializer.parse(json)` → `player.sendMessage(BaseComponent[])`

### 2. Core.onEnable/onDisable — Bukkit.getScheduler()
```
java.lang.UnsupportedOperationException
    at org.bukkit.craftbukkit.scheduler.CraftScheduler.handle
```
Original: `Bukkit.getScheduler().runTaskLater(...)` / `.cancelTasks(...)`  
Patched: `Bukkit.getGlobalRegionScheduler().runDelayed(...)` / `.cancelTasks(...)`  
Async task: `Bukkit.getAsyncScheduler().runAtFixedRate(...)`

---

## Patching Approach (Two-Step)

We use **two complementary approaches** because different classes need different strategies:

### Step 1: FancyMessage — Bytecode Patcher (Javassist)
Replaces the `send()` method body directly in bytecode.

```bash
cd /root/github/ae/bytecode-patcher
javac -cp javassist.jar -d out src/FancyMessagePatcher.java
java -cp "out:javassist.jar:../libs/canvas-api.jar:../FoliaPatch/libs/bungeecord-chat.jar" \
    FancyMessagePatcher \
    ../AdvancedEnchantments-9.22.7.jar \
    /tmp/ae-temp-patched.jar
```

### Step 2: Core — Compile + Inject
Compile the patched `Core.java` source from FoliaPatch against the original jar, then inject.

```bash
cd /root/github/ae
CP="AdvancedEnchantments-9.22.7.jar"
for j in FoliaPatch/libs/*.jar; do CP="$CP:$j"; done
javac -cp "$CP" -d /tmp/core-patch FoliaPatch/src/main/java/net/advancedplugins/ae/Core.java

# Inject into the FancyMessage-patched jar
cp /tmp/ae-temp-patched.jar AdvancedEnchantments-9.22.7-folia-patched.jar
jar uf AdvancedEnchantments-9.22.7-folia-patched.jar -C /tmp/core-patch net/advancedplugins/ae/Core.class
```

### Step 3: Deploy
```bash
bash deploy.sh
```

---

## One-Command Rebuild

```bash
cd /root/github/ae && \
  cd bytecode-patcher && \
  javac -cp javassist.jar -d out src/FancyMessagePatcher.java && \
  java -cp "out:javassist.jar:../libs/canvas-api.jar:../FoliaPatch/libs/bungeecord-chat.jar" \
    FancyMessagePatcher ../AdvancedEnchantments-9.22.7.jar /tmp/ae-temp-patched.jar && \
  cd .. && \
  CP="AdvancedEnchantments-9.22.7.jar" && \
  for j in FoliaPatch/libs/*.jar; do CP="$CP:$j"; done && \
  javac -cp "$CP" -d /tmp/core-patch FoliaPatch/src/main/java/net/advancedplugins/ae/Core.java && \
  cp /tmp/ae-temp-patched.jar AdvancedEnchantments-9.22.7-folia-patched.jar && \
  jar uf AdvancedEnchantments-9.22.7-folia-patched.jar -C /tmp/core-patch net/advancedplugins/ae/Core.class && \
  bash deploy.sh && \
  echo "=== All patches applied and deployed ==="
```

---

## Directory Structure

```
ae/
├── AdvancedEnchantments-9.22.7.jar              # Original plugin jar (source)
├── AdvancedEnchantments-9.22.7-folia-patched.jar # Output: patched jar
├── deploy.sh                                     # Deploy script to server
│
├── bytecode-patcher/
│   ├── javassist.jar                             # Javassist library
│   ├── out/                                      # Compiled patcher .class
│   └── src/
│       └── FancyMessagePatcher.java              # Patches FancyMessage.send()
│
├── FoliaPatch/
│   ├── src/main/java/net/advancedplugins/ae/
│   │   ├── Core.java                             # Patched Core (Folia schedulers)
│   │   └── impl/utils/fanciful/FancyMessage.java # Reference FancyMessage patch
│   ├── src/main/resources/_extracted/            # Extracted resources from original jar
│   ├── libs/                                     # All dependency jars for compilation
│   └── pom.xml                                   # Maven build (optional, has issues)
│
├── decompiled/                                   # Full Vineflower decompilation (reference only)
│
└── libs/
    └── canvas-api.jar                            # Canvas/Folia API
```

---

## What Each Patched File Does

### Core.java (FoliaPatch/src/main/java/...)
Replaces all `Bukkit.getScheduler()` calls in `onEnable()` and `onDisable()`:

| Original | Patched |
|----------|---------|
| `Bukkit.getScheduler().runTaskLater(plugin, runnable, delay)` | `Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delay)` |
| `new BukkitRunnable().runTaskTimerAsynchronously(plugin, delay, period)` | `Bukkit.getAsyncScheduler().runAtFixedRate(plugin, consumer, delayMs, periodMs, MILLISECONDS)` |
| `new BukkitRunnable().runTask(plugin)` | `Bukkit.getGlobalRegionScheduler().run(plugin, t -> ...)` |
| `Bukkit.getScheduler().cancelTasks(plugin)` | `Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)` |

### FancyMessage.java (bytecode patched)
Replaces `send(CommandSender, String)`:

| Original | Patched |
|----------|---------|
| `Bukkit.dispatchCommand(console, "tellraw " + player + " " + json)` | `ComponentSerializer.parse(json)` → `player.sendMessage(parts)` |

---

## Verify Patches

```bash
cd /root/github/ae

# Check FancyMessage — should show ComponentSerializer, NOT dispatchCommand
unzip -p AdvancedEnchantments-9.22.7-folia-patched.jar \
    net/advancedplugins/ae/utils/fanciful/FancyMessage.class > /tmp/fm.class
javap -c -p /tmp/fm.class | grep -i "ComponentSerializer\|dispatchCommand"

# Check Core — should show getGlobalRegionScheduler, NOT getScheduler
unzip -p AdvancedEnchantments-9.22.7-folia-patched.jar \
    net/advancedplugins/ae/Core.class > /tmp/core.class
javap -c -p /tmp/core.class | grep -i "getGlobalRegionScheduler\|getAsyncScheduler\|getScheduler"
```

Expected: FancyMessage shows `ComponentSerializer.parse`, Core shows `getGlobalRegionScheduler`/`getAsyncScheduler`.  
**No `getScheduler` or `dispatchCommand` should appear.**

---

## Updating to a New Plugin Version

When AdvancedEnchantments releases a new version:

1. **Download the new original jar** → place as `AdvancedEnchantments-NEW.jar`
2. **Test if FancyMessage still needs patching**:
   ```bash
   javap -c -p <(unzip -p AdvancedEnchantments-NEW.jar \
       net/advancedplugins/ae/utils/fanciful/FancyMessage.class) | grep dispatchCommand
   ```
   If it shows `dispatchCommand`, the patch is still needed.
3. **Decompile the new jar** and check `Core.java` for `getScheduler()` calls
4. **Update FoliaPatch/src/.../Core.java** to match the new version's code (replace scheduler calls)
5. **Rebuild** using the one-command rebuild above
6. **Test** on server

---

## Troubleshooting

### `UnsupportedOperationException` from CraftScheduler
Core.onEnable/onDisable patch not applied. Rebuild and check:
```bash
javap -c -p /tmp/core.class | grep getScheduler
```
Should be **empty** (no results).

### `Dispatching command async` error
FancyMessage patch not applied. Rebuild and check:
```bash
javap -c -p /tmp/fm.class | grep dispatchCommand
```
Should be **empty** (no results).

### Compile errors when building Core.java
Add missing libs to `FoliaPatch/libs/`. The Core.java source references many internal classes from the original jar, so `AdvancedEnchantments-9.22.7.jar` MUST be on the classpath.

### Plugin still has other Folia issues
Check server logs. Some AE features (tinkerer, alchemist, armor sets) may have additional threading issues. See `FoliaPatch/FOLIA_NOTES.md` for known risky areas.
