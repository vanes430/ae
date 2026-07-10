# Folia Concurrency Analysis

Safety analysis for the highest-risk patched modules.

---

## Inventory Interactions (Tinkerer & Market)

**Risk:** Concurrent inventory clicks on different region threads corrupt shared state → item duplication or loss.

### TinkererInventory
- `inventoryMap` → `ConcurrentHashMap`
- `successfulTrades` → `Collections.synchronizedList`
- All modifications run on event listener thread (player's region thread)

### MarketInventory
- `enchantHashmap` → `Collections.synchronizedMap(new LinkedHashMap<>())` (async cache writes, sync reads)
- Paginated key extraction protected by `synchronized` block

---

## Repeating Enchantment Tickers (RepeatingTrigger)

**Risk:** Timer-based armor enchants on global scheduler access player inventory on wrong thread → packet failure.

### Fix
- Tickers scheduled on player's entity scheduler: `entity.getScheduler().runAtFixedRate(...)`
- Active tasks tracked per slot via Folia `ScheduledTask`
- Tasks cancelled on armor swap, world change, drop, or quit
- Global `repeaters` map → `ConcurrentHashMap`

---

## Armor Attribute Updates (ArmorWearTrigger / ArmorListener)

**Risk:** Global debounced health update calls `.getCurrentRegionizedWorldData()` on wrong thread → server crash.

### Fix
- Removed all global/SchedulerUtils task queues
- Debouncing and health recalculation on player's entity scheduler:
```java
player.getScheduler().runDelayed(plugin, task -> {
    if (player.isOnline()) {
        ASManager.resetPlayerHealth(player, health);
    }
}, null, ticks);
```
