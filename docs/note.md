# Folia Compatibility Notes - AdvancedEnchantments 9.22.7

## ⚠️ DANGEROUS / HARD - JANGAN DIPAKSA FOLIA COMPATIBLE
Effect/class berikut berinteraksi dengan **inventory operations** yang sangat riskan untuk di-patch ke Folia.
Jika salah scheduler choice → bisa menyebabkan **item dupe, item loss, atau inventory corruption**.

### Inventory Interactions (HIGH RISK - DUPE POTENTIAL)
| File | Scheduler Call | Reason |
|------|---------------|--------|
| `TinkererInventory.java` | `runTaskLater` on `InventoryCloseEvent` → gives items back | Inventory close + item give = race condition. Wrong region → dupe or item loss |
| `AlchemistInventoryClicks.java` | `runTaskLater` on inventory close → returns items, re-opens inventory | Same as above + inventory re-open = very risky |
| `AdminChatListener.java` | `runTask` → opens `AdminInventory` for player | Inventory open tied to player region |
| `ArmorWearTrigger.java` | `runTaskLater` → checks helmet slot, reads inventory | Inventory read during armor switch = timing sensitive |

### Entity Tracking (HIGH RISK)
| File | Scheduler Call | Reason |
|------|---------------|--------|
| `ChangeArrow.java` | `runTaskTimer` → entity teleport every tick + `cancelTask` | Entity tracking with 1-tick period. Projectiles move between regions constantly. Very hard to schedule correctly. |
| `Homing.java` | `runTaskTimer` → modifies projectile velocity every tick | Projectile moves across regions. EntityScheduler might not be available if projectile dies mid-task. |
| `PacketChangeArrow.java` | `runTaskLater` → projectile removal | Entity removal after hit. Might work but risky with packet-based arrow changes. |
| `RepeatingTrigger.java` | `runTaskTimer` → repeating enchantment effects via `BukkitTask` | Tasks stored in HashMap, cancelled on world change/drop. Complex lifecycle. |

### Death/Revival Effects (MEDIUM-HIGH RISK)
| File | Scheduler Call | Reason |
|------|---------------|--------|
| `DamageHandler.java` | `BukkitRunnable.runTaskLater` → player teleport after respawn | Player respawn = complex state. Teleport after respawn needs proper entity scheduling. |
| `PumpkinDeathListener.java` | `BukkitRunnable.runTaskLater` → unregister listener + inventory manipulation on death | Player death + drops + inventory helmet slot manipulation = risky |
| `BleedEffect.java` | `BukkitRunnable.runTaskTimer` → damage over time on player | EntityScheduler needed, but entity might die mid-task |
| `ReviveEffect.java` | `BukkitRunnable.runTaskLater` → revive dead player | Player revival = complex state transition |
| `IgnoreArmorDamageEffect.java` | `scheduleSyncDelayedTask` (deprecated) | Effect activation timing |

### Other Effects (MEDIUM RISK)
| File | Scheduler Call | Reason |
|------|---------------|--------|
| `LavaWalkerEffect.java` | `runTaskLater` / `cancelTask` → effect on boots while walking | Entity-bound, might work with EntityScheduler |
| `WaterWalkerEffect.java` | `runTaskLater` / `cancelTask` → effect on boots while walking | Same as LavaWalker |
| `AddWalkSpeedEffect.java` | `cancelTask` → removes speed boost | Entity-bound |
| `GuardEffect.java` | `runTaskLater` → armor effect activation | Entity-bound |
| `PumpkinEffect.java` | `runTaskLater` → helmet effect | Entity-bound, inventory interaction |
| `BoostEffect.java` | `runTask` → applies potion effect | Entity-bound |
| `ConsoleCommandEffect.java` | `runTask` → dispatches console command | Global task, low risk |

### Effect Execution Engine (COMPLEX)
| File | Scheduler Call | Reason |
|------|---------------|--------|
| `ExecutionTask.java` | `BukkitRunnable.runTaskLater` → delayed WAIT effects | Core effect execution engine. Effects can target entities, locations, blocks. Very complex to schedule correctly. |

---

## ✅ SAFE TO PATCH
Scheduler calls berikut **aman** untuk di-patch karena tidak berinteraksi dengan inventory atau entity state yang kompleks.

### Global / Plugin-level tasks
| File | Line | Original Call | Target Scheduler | Description |
|------|------|--------------|-----------------|-------------|
| `MarketInventory.java` | 62 | `BukkitRunnable.runTaskTimerAsynchronously` | `AsyncScheduler.runAtFixedRate` | Async cache of market enchantments |
| `MarketInventory.java` | 72 | `Bukkit.getScheduler().cancelTask` | Cancel async task ID | Cancel market cache task |
| `ReloadEvent.java` | 14 | `Bukkit.getScheduler().runTaskLater` | `GlobalRegionScheduler.runDelayed` | Empty player iteration on reload (harmless) |
| `MainCommand.java` | 270 | `Bukkit.getScheduler().runTaskAsynchronously` | `AsyncScheduler.runNow` | PlInfo async link creation |
| `MainCommand.java` | 281 | `Bukkit.getScheduler().runTaskAsynchronously` | `AsyncScheduler.runNow` | Zip creation async |

---

## Folia Scheduler Decision Guide
Di Folia/Canvas, ada 4 jenis scheduler:

1. **GlobalRegionScheduler** — Task global yang tidak terikat entity/player tertentu. Contoh: broadcast, config reload, market cache.
2. **EntityScheduler** (via `entity.getScheduler()`) — Task yang terikat entity spesifik. Dijamin execute di region yang sama dengan entity. Contoh: damage, teleport, potion effect.
3. **RegionScheduler** — Task terikat world+chunk spesifik. Contoh: block placement, world population.
4. **AsyncScheduler** — Task async yang tidak terikat region. Contoh: file I/O, HTTP requests, database queries.

### Rules:
- Kalau task **interaksi dengan Player** (inventory, teleport, message) → `player.getScheduler().execute()` atau `GlobalRegionScheduler` (kalau player online check)
- Kalau task **interaksi dengan Entity** (damage, velocity, AI) → `entity.getScheduler().execute()`
- Kalau task **interaksi dengan Block/World** → `RegionScheduler`
- Kalau task **tidak interact dengan game state** (file I/O, HTTP) → `AsyncScheduler`
- Kalau task **global/plugin level** → `GlobalRegionScheduler`
