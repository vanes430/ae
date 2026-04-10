# TO_DO — Unpatched Classes

Classes below still use old Bukkit scheduler APIs (`BukkitRunnable`, `Bukkit.getScheduler()`) and may cause `UnsupportedOperationException` on Folia/Canvas servers.

---

## ✅ Already Patched

| Class | Scheduler Used | Status |
|-------|---------------|--------|
| `Core.java` + inner classes | `GlobalRegionScheduler`, `AsyncScheduler` | ✅ |
| `MarketInventory.java` | `AsyncScheduler.runAtFixedRate()` | ✅ |
| `ReloadEvent.java` | `GlobalRegionScheduler.runDelayed()` | ✅ |
| `UpdateChecker.java` | No scheduler needed | ✅ |
| `ApplyPotionEffect.java` | `EntityScheduler.execute()` | ✅ |
| `GuardEffect.java` | `EntityScheduler.execute()` | ✅ |

---

## ⚠️ HIGH RISK — Item Dupe / Inventory Corruption Potential

These classes interact with inventory operations. Wrong scheduler choice → item dupe, item loss, or inventory corruption.

| Class | Issue | Scheduler Call |
|-------|-------|---------------|
| `TinkererInventory.class` | Inventory close → gives items back | `runTaskLater` |
| `AlchemistInventoryClicks.class` | Inventory close + reopen | `runTaskLater` |
| `ArmorWearTrigger.class` | Inventory read during armor switch | `runTaskLater` |
| `AdminChatListener.class` | Opens inventory for player | `runTask` |
| `ChangeArrow.class` | Projectile entity tracking every tick | `runTaskTimer` + `cancelTask` |
| `PacketChangeArrow.class` | Projectile removal after hit | `runTaskLater` |

---

## 🔶 MEDIUM-HIGH RISK — Complex Entity Lifecycle

| Class | Issue | Scheduler Call |
|-------|-------|---------------|
| `Homing.class` + inner | Projectile velocity modification every tick | `runTaskTimer` |
| `RepeatingTrigger.class` + inner | Repeating enchantment effects, complex lifecycle | `runTaskTimer` via `BukkitTask` |
| `DamageHandler.class` + inner | Player teleport after respawn | `BukkitRunnable.runTaskLater` |
| `PumpkinDeathListener.class` + inner | Death + drops + helmet slot manipulation | `BukkitRunnable.runTaskLater` |
| `BleedEffect.class` + inner | Damage over time on player | `BukkitRunnable.runTaskTimer` |
| `ReviveEffect.class` + inner | Revive dead player | `BukkitRunnable.runTaskLater` |
| `IgnoreArmorDamageEffect.class` | Effect activation timing | `scheduleSyncDelayedTask` (deprecated) |
| `ExecutionTask.class` + inner | Core effect execution engine | `BukkitRunnable.runTaskLater` |

---

## 🟡 MEDIUM RISK — Entity-Bound Effects

| Class | Issue | Scheduler Call |
|-------|-------|---------------|
| `LavaWalkerEffect.class` | Effect on boots while walking | `runTaskLater` / `cancelTask` |
| `WaterWalkerEffect.class` | Effect on boots while walking | `runTaskLater` / `cancelTask` |
| `AddWalkSpeedEffect.class` | Removes speed boost | `cancelTask` |
| `PumpkinEffect.class` | Helmet effect | `runTaskLater` |
| `BoostEffect.class` | Applies potion effect | `runTask` |
| `ConsoleCommandEffect.class` | Dispatches console command | `runTask` |
| `MainCommand.class` | `/ae zip` and `/ae plinfo` async | `runTaskAsynchronously` |

---

## How to Patch

### For Inventory Interactions (HIGH RISK):
Use `player.getScheduler().execute()` for inventory operations, or `GlobalRegionScheduler` with proper sync checks.

### For Entity/Projectile Tracking (MEDIUM-HIGH RISK):
Use `entity.getScheduler().execute()` or `EntityScheduler`. Check if entity is still valid before executing.

### For Simple Effects (MEDIUM RISK):
- Entity-bound effects → `entity.getScheduler().execute()`
- Global tasks → `GlobalRegionScheduler`
- Async/file I/O → `AsyncScheduler`

### Steps:
1. Add source to `src-patched/main/java/<package-path>.java`
2. Replace scheduler calls with Folia-compatible alternatives
3. Add exclude pattern in `build.gradle.kts` if needed (for inner classes)
4. `./gradlew clean deploy`
5. Test on server — watch for item dupe, inventory corruption, or entity tracking bugs

---

## Notes

- Don't patch blindly — test each change individually
- Inventory operations are the most dangerous — one wrong scheduler = item dupe
- Projectile/entity tracking is tricky because entities move between regions
- Effects stored in HashMaps with complex lifecycles need careful handling
- If a feature works fine on server (no errors), consider leaving it unpatched
