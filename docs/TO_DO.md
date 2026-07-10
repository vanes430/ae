# Verification Checklist

All Folia compatibility migrations for **AdvancedEnchantments v9.23.6** are complete.

---

## Post-Deploy Tests

### Combat & Teleportation
- [ ] Trigger combat enchantments (Vampire, Lifesteal, TeleportBehind)
- [ ] Health updates without `NullPointerException` on attribute packets
- [ ] Teleport effects work without threading exceptions

### Repeating Enchantments
- [ ] Equip gear with repeating effects (Glowing, Implants, Haste)
- [ ] Effects apply periodically and stop on unequip
- [ ] No `CraftScheduler` or `ThreadedRegion` exceptions in console

### Tinkerer & Trading
- [ ] Open Tinkerer (`/ae tinker`) with multiple players
- [ ] Items safely returned on close — no loss or duplication
- [ ] Concurrent trades don't leak state between players

### Block Manipulation
- [ ] Mining enchantments (Trench, Smelting) work
- [ ] Block breakage, placement, drops execute smoothly
- [ ] No async chunk loading warnings in console
