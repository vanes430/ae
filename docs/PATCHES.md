# Patch Reference

Base version: **AdvancedEnchantments 9.24.2**
Patches: **27** (all apply clean, 0 failures)
Upgraded from 9.23.6 — Jul 30 2026

Complete inventory of all 27 Folia compatibility patches. Each patch migrates Bukkit scheduler calls to Folia region-aware equivalents.

---

## Patch Index

### 0001 — Core.java
- **File:** `net/advancedplugins/ae/Core.java`

### 0002 — AlchemistInventoryClicks.java
- **File:** `net/advancedplugins/ae/features/alchemist/AlchemistInventoryClicks.java`

### 0003 — ArmorListener.java
- **File:** `net/advancedplugins/ae/impl/effects/armorutils/ArmorListener.java`

### 0004 — ExecutionTask.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/actions/execution/ExecutionTask.java`

### 0005 — DamageHandler.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/actions/handlers/DamageHandler.java`

### 0006 — ApplyPotionEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/ApplyPotionEffect.java`

### 0007 — BoostEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/BoostEffect.java`

### 0008 — ExtinguishEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/ExtinguishEffect.java`

### 0009 — FireballEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/FireballEffect.java`

### 0010 — GuardEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/GuardEffect.java`

### 0011 — TeleportBehindEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/TeleportBehindEffect.java`

### 0012 — TeleportEffect.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/effects/internal/TeleportEffect.java`

### 0013 — ArmorWearTrigger.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal/ArmorWearTrigger.java`

### 0014 — RepeatingTrigger.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal/RepeatingTrigger.java`

### 0015 — UserRepeaters.java
- **File:** `net/advancedplugins/ae/impl/effects/effects/mechanics/triggers/internal/UserRepeaters.java`

### 0016 — FancyMessage.java (utils)
- **File:** `net/advancedplugins/ae/utils/fanciful/FancyMessage.java`

### 0017 — AdminChatListener.java
- **File:** `net/advancedplugins/ae/globallisteners/listeners/AdminChatListener.java`

### 0018 — ReloadEvent.java
- **File:** `net/advancedplugins/ae/globallisteners/listeners/ReloadEvent.java`

### 0019 — MarketInventory.java
- **File:** `net/advancedplugins/ae/handlers/netsharing/MarketInventory.java`

### 0020 — TinkererInventory.java
- **File:** `net/advancedplugins/ae/features/tinkerer/TinkererInventory.java`

### 0021 — FoliaScheduler.java
- **File:** `net/advancedplugins/ae/impl/utils/FoliaScheduler.java`

### 0022 — ReallyFastBlockHandler.java
- **File:** `net/advancedplugins/ae/impl/utils/ReallyFastBlockHandler.java`

### 0023 — SchedulerUtils.java
- **File:** `net/advancedplugins/ae/impl/utils/SchedulerUtils.java`

### 0024 — FancyMessage.java (impl)
- **File:** `net/advancedplugins/ae/impl/utils/fanciful/FancyMessage.java`

### 0025 — MinecraftVersion.java
- **File:** `net/advancedplugins/ae/impl/utils/nbt/utils/MinecraftVersion.java`

### 0026 — UpdateChecker.java
- **File:** `net/advancedplugins/ae/impl/utils/plugin/UpdateChecker.java`

### 0027 — AdvancedWeapon.java
- **File:** `net/advancedplugins/ae/features/weapons/AdvancedWeapon.java`

---

## Patch Generation

Patches are auto-generated via `tools/scripts/generate-patch.sh` or batch-regenerated via `tools/scripts/regenerate-patches.sh`. Headers (Subject, AE PATCH REASON/FIX/RISK) are filled manually.

Patches apply on top of `src-decompiled/` via `git apply --directory=src-patched`. The `a/` and `b/` paths in each patch are relative to `src-patched/` and `src-decompiled/` respectively.
