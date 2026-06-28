# Project Status & Verification Checklist

All planned Folia compatibility migrations, scheduler adaptations, and concurrency/thread-safety patches for **AdvancedEnchantments v9.23.6** have been **fully completed**. 

This document serves as the completion log and verification checklist to test the patched plugin on live Folia/Canvas servers.

---

## 🏁 Completed Migrations

### 1. Core Lifecycle & Global Tasks (Low Risk)
* **Core.java:** Plugin disable task cancellation migrated to direct global and async schedulers.
* **ReloadEvent.java:** Delay task migrated to `Bukkit.getGlobalRegionScheduler()`.
* **UpdateChecker.java:** Checking tasks migrated to `Bukkit.getAsyncScheduler()`.
* **MarketInventory.java:** Repeating database cache tasks migrated to `Bukkit.getAsyncScheduler().runAtFixedRate(...)`.

### 2. Entity Effects & Combat Interactions (Medium Risk)
* **BoostEffect.java:** Scheduled velocity modifiers on target's entity scheduler.
* **ExtinguishEffect.java:** Scheduled entity fire state changes on entity scheduler.
* **TeleportBehindEffect.java:** Teleports migrated to asynchronous execution (`target.teleportAsync(...)`).
* **GuardEffect.java:** Guard entities spawn and removal cleanup scheduled on entity's regional tick thread.
* **ExecutionTask.java:** Delayed ability effects (`WAIT` action) migrated to run on target entity's scheduler or region scheduler.

### 3. Inventory Operations & Tracking (High Risk)
* **TinkererInventory.java:** Migrated open tinkerer inventories list to thread-safe `ConcurrentHashMap` and player trade tracking to synchronized lists.
* **ArmorListener.java:** Rewrote the global debounced armor update queue to use individual player entity schedulers (`player.getScheduler()`).
* **ArmorWearTrigger.java:** Scheduled armor switch checks and player health attributes updates on player's region thread, resolving the `NullPointerException` inside attribute packets.
* **RepeatingTrigger.java:** Migrated repeating enchantment tickers to the target entity's tick thread (`entity.getScheduler().runAtFixedRate(...)`).

### 4. Concurrency & Thread Safety
* **ReallyFastBlockHandler.java:** Eliminated all old NMS/ClassWrapper reflections and implemented thread-safe world handlers lookup via `ConcurrentHashMap` and atomic block changes on region schedulers.
* **FancyMessage.java:** Catch blocks updated to handle new library signatures without Checked Exception mismatch.

---

## 🧪 Post-Deploy Verification Checklist

Run these test cases on a Folia server to verify that the patched plugin is fully stable:

### 1. Combat & Teleportation
* [ ] Trigger combat enchantments (e.g. `Vampire`, `Lifesteal`, `TeleportBehind`).
* [ ] Verify that player health increases/decreases without throwing `NullPointerException` (associated with health attributes updates).
* [ ] Verify that teleport effects do not throw regionized threading exceptions.

### 2. Repeating Enchantments
* [ ] Equip gear containing repeating effects (e.g. `Glowing`, `Implants`, `Haste`).
* [ ] Verify that repeating effects apply periodically (every second / tick) and terminate instantly when the armor is unequipped.
* [ ] Check server console logs for any `CraftScheduler` or `ThreadedRegion` exceptions during repeating cycles.

### 3. Tinkerer & Trading
* [ ] Open Tinkerer menu (`/ae tinker`) with multiple players simultaneously.
* [ ] Place items inside the trade grid and close the inventory.
* [ ] Verify items are safely returned to player inventories without item loss or duplication.
* [ ] Verify that Concurrent trades do not leak states between different players.

### 4. Block Manipulation
* [ ] Trigger mining/tool enchantments (e.g. `Trench`, `Smelting`, `VeinMiner`).
* [ ] Verify block breakage, block placement, and drops generation execute smoothly.
* [ ] Check console to ensure no async chunk loading or block modification warnings are printed.
