# Folia Concurrency & Safety Analyses

This document details the safety locks, thread mapping, and concurrency implementations applied to the highest-risk modules of AdvancedEnchantments.

---

## 🛡️ Inventory Interactions (Tinkerer & Market)

### The Risk
In legacy Bukkit, inventory opens/closes and packet data modification are executed synchronously on a single main thread. In Folia, inventory clicks and player transactions execute concurrently on the respective player's region thread. Writing to a shared map or modifying items without locks will cause items duplication (dupe glitch) or inventory wiping.

### Patched Implementation
1. **Tinkerer Inventory State (`TinkererInventory.java`):**
   * Shared inventories tracker `inventoryMap` migrated to `ConcurrentHashMap`.
   * Trade completion status `successfulTrades` migrated to `Collections.synchronizedList`.
   * All inventory modifications (adding, returning, clearing) are executed strictly inside the event listener thread (which runs on the player's region thread).
2. **Global Market Cache (`MarketInventory.java`):**
   * Migrated `enchantHashmap` cache map to `Collections.synchronizedMap(new LinkedHashMap<>())` because it is written to asynchronously by the update checker/cache daemon, and read synchronously by players opening the page.
   * Paginated queries are protected by a synchronization lock during key extraction:
     ```java
     synchronized (enchantHashmap) {
         keys = new ArrayList<>(enchantHashmap.keySet());
     }
     ```

---

## ⚡ Repeating Enchantment Tickers (`RepeatingTrigger.java`)

### The Risk
Armor enchants (like speed, regeneration, or glowing) run on a task timer. If scheduled on the global scheduler, they will attempt to check player inventories and apply potion effects on the wrong thread context, resulting in attribute updates packet failure.

### Patched Implementation
1. **Entity-Specific Ticker:**
   * Tickers are scheduled directly on the player's entity scheduler (`finalEntity.getScheduler().runAtFixedRate(...)`).
   * Tickers execute safely on the player's active region thread.
2. **Safe Task Dereferencing:**
   * Active tasks are tracked per equipment slot in `UserRepeaters` using the native Folia `ScheduledTask` class.
   * Tasks are safely cancelled (`ScheduledTask.cancel()`) on armor swap, world change, drop, or player quit.
3. **Thread-Safe Registries:**
   * Global repeaters registry map `repeaters` migrated to `ConcurrentHashMap`.

---

## 🩸 Armor Attribute & Packet Updates (`ArmorWearTrigger.java` / `ArmorListener.java`)

### The Risk
When players swap armor, health attributes are updated. In legacy Bukkit, the plugin debounced this update on a global Bukkit task, leading to attribute packet creation calling `.getCurrentRegionizedWorldData()` on the global region scheduler thread (where no world data is bound), resulting in a server-wide crash (`NullPointerException`).

### Patched Implementation
* Removed all global/SchedulerUtils task queues.
* Debouncing and health attribute calculations are now scheduled directly on the player's entity scheduler:
  ```java
  player.getScheduler().runDelayed(plugin, task -> {
      if (player.isOnline()) {
          // Safe to reset health attributes and send attributes update packet
          ASManager.resetPlayerHealth(player, health);
      }
  }, null, ticks);
  ```
