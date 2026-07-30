# Testing Checklist

Verify Folia compatibility after build. All tests run on the Folia server with the patched JAR deployed.

---

## Smoke Test

```bash
./gradlew clean build    # Must compile with zero errors
./gradlew deploy         # Must copy JAR successfully
```

Server startup: no `CraftScheduler` or `ThreadedRegion` exceptions in console on load.

---

## Combat & Teleportation

- [ ] Trigger combat enchantments: Vampire, Lifesteal, TeleportBehind
- [ ] Health updates work — no `NullPointerException` on attribute packets
- [ ] Teleport effects execute without threading exceptions
- [ ] DamageHandler processes damage on correct region thread

---

## Repeating Enchantments

- [ ] Equip gear with repeating effects: Glowing, Implants, Haste
- [ ] Effects apply periodically at correct intervals
- [ ] Effects stop immediately on unequip
- [ ] No `CraftScheduler` or `ThreadedRegion` exceptions in console
- [ ] Multiple players with repeating enchants — no cross-player state leak

---

## Tinkerer & Trading

- [ ] Open Tinkerer (`/ae tinker`) — GUI renders correctly
- [ ] Multiple concurrent Tinkerer sessions — no inventory corruption
- [ ] Items safely returned on close — no loss or duplication
- [ ] Trades complete atomically

---

## Market

- [ ] Open Market GUI — enchantment list populates
- [ ] Pagination works (synchronized key iteration)
- [ ] Buy/sell operations complete on player's region thread

---

## Block Manipulation

- [ ] Mining enchantments: Trench, Smelting, Tunnel
- [ ] Block break, placement, drops execute correctly
- [ ] No async chunk loading warnings

---

## Alchemist

- [ ] Open Alchemist GUI — inventory renders
- [ ] Click handlers process on correct thread
- [ ] Concurrent alchemist sessions — no state corruption

---

## Admin / Global

- [ ] `/ae reload` — uses GlobalRegionScheduler, no errors
- [ ] Admin chat listener — messages delivered
- [ ] Update checker — runs on AsyncScheduler, doesn't block startup

---

## Common Failure Patterns

| Symptom | Likely Cause |
|---------|-------------|
| `CraftScheduler` exception | A Bukkit scheduler call wasn't patched |
| `ThreadedRegion` exception | Entity action ran on wrong region thread |
| `NullPointerException` on health | Health update ran async, player logged out |
| Item duplication/loss | Concurrent inventory access without synchronization |
| Repeating effect doesn't stop | ScheduledTask not cancelled on unequip |
| Console spam on startup | GlobalRegionScheduler used where entity scheduler needed |