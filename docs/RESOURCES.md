# Resource Overrides

Files in `resources/` are overlaid onto the patched JAR at build time, replacing the originals from the decompiled plugin.

---

## Override Inventory

| Path | Purpose |
|------|---------|
| `plugin.yml` | Plugin descriptor (may need version bump on upgrade) |
| `enchantments.yml` | Enchantment definitions, levels, applicable items |
| `armorSets/` | Custom armor set configurations |
| `customWeapons/` | Custom weapon configurations |

---

## Build Integration

In `build.gradle.kts`, the JAR task layers resources last:

```kotlin
// Layer 3: Resource overrides
from("resources")
```

This means any file in `resources/` with the same path as one inside the original JAR will take precedence.

---

## enchantments.yml Summary

Source defines all enchantments, their max levels, and which items they apply to.

| Category | Count | Example Enchants |
|----------|-------|-----------------|
| ALL_ARMOR | 30 | armored, reflect, dodge, immortal, hardened |
| ALL_HELMET | 4 | glowing, implants, aquatic, smokebomb |
| ALL_CHESTPLATE | 5 | angelic, chunky, phoenix, shockwave, fumble |
| ALL_LEGGINGS | 2 | explosivedemise, extinguish |
| ALL_BOOTS | 12 | gears, springs, wings, lavawalker, waterwalker |
| SWORDS | 41 | lifesteal, vampire, decapitation, disarm, execute |
| AXES | 27 | bleed, cleave, timber, barbarian, shatter |
| BOW | 15 | explosive, homing, sniper, longbow, missile |
| CROSSBOW | 2 | marksman, strike |
| TRIDENT | 3 | impact, strike, twinge |
| TOOLS | 15 | smelting, trench, tunnel, haste, magnet, telepathy |
| ELYTRA | 3 | momentum, rush, slingshot |
| FISHING_ROD | 7 | allure, bait, autoreel, hook, lucky |

### Slot Mapping

| Equipment Slot | Available Enchant Pools |
|---------------|------------------------|
| Helmet | ALL_ARMOR + ALL_HELMET |
| Chestplate | ALL_ARMOR + ALL_CHESTPLATE |
| Leggings | ALL_ARMOR + ALL_LEGGINGS |
| Boots | ALL_ARMOR + ALL_BOOTS |

---

## When to Modify

- **Balance changes** → edit `enchantments.yml` (max levels, applicable items)
- **New enchantment** → add definition to `enchantments.yml`
- **Plugin metadata change** → edit `plugin.yml`
- **Custom armor/weapon set** → add YAML to `armorSets/` or `customWeapons/`