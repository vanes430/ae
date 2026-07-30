#!/usr/bin/env python3
"""
Check armor set and custom weapon YML files for enchant references
that don't exist in enchantments.yml.
Only reports custom AE enchants (vanilla Minecraft enchants are excluded).
"""

import yaml, os, re
from pathlib import Path

CUSTOM_EFFECTS = {"increase_damage", "decrease_damage", "experience"}

# Vanilla Minecraft enchants - these work without AE config
VANILLA_ENCHANTS = {
    "protection", "fire_protection", "blast_protection", "projectile_protection",
    "feather_falling", "respiration", "aqua_affinity", "thorns",
    "depth_strider", "frost_walker", "binding_curse", "soul_speed",
    "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect",
    "looting", "sweeping_edge", "sweeping", "mending", "unbreaking",
    "efficiency", "fortune", "silk_touch",
    "power", "punch", "flame", "infinity",
    "luck_of_the_sea", "lure",
    "impaling", "riptide", "loyalty", "channeling",
    "quick_charge", "multishot", "piercing",
    "vanishing_curse", "swift_sneak",
    # Bukkit API names for the same
    "arrow_damage", "arrow_fire", "arrow_infinite", "arrow_knockback",
    "damage_all", "damage_arthropods", "damage_undead",
    "dig_speed", "durability", "loot_bonus_blocks", "loot_bonus_mobs",
    "oxygen", "water_worker", "water_breathing",
}

def main():
    enchants = yaml.safe_load(open("resources/enchantments.yml"))
    ae_enchants = {k.lower(): k for k in enchants.keys()}  # lowercase lookup

    # Collect all enchant references from armor/weapon files
    file_refs = {}
    for root, dirs, files in os.walk("resources"):
        for f in files:
            fpath = Path(root) / f
            if not f.endswith(".yml") or str(fpath) == "resources/enchantments.yml":
                continue
            with open(fpath) as fh:
                try:
                    data = yaml.safe_load(fh)
                except:
                    continue
            if not isinstance(data, dict):
                continue

            refs = []
            def scan(obj):
                if isinstance(obj, dict):
                    for v in obj.values(): scan(v)
                elif isinstance(obj, list):
                    for item in obj: scan(item)
                elif isinstance(obj, str):
                    m = re.match(r"^([a-zA-Z_]+):(\d+)$", obj.strip().strip("'\""))
                    if m:
                        refs.append((m.group(1).lower(), m.group(1), m.group(2)))
            scan(data)
            if refs:
                file_refs[fpath] = refs

    # Categorize
    total_missing = 0
    for fpath in sorted(file_refs):
        items = file_refs[fpath]
        missing = []
        for key, orig_name, level in items:
            if key in VANILLA_ENCHANTS or key in CUSTOM_EFFECTS:
                continue  # skip vanilla or non-enchant custom effects
            if key in ae_enchants:
                continue  # exists in AE config
            missing.append((orig_name, level))

        if missing:
            print(f"\n  {fpath.relative_to('resources')}")
            for name, level in sorted(set(missing)):
                print(f"    - {name}:{level}")
                total_missing += 1

    print(f"\n{'='*60}")
    print(f"Total missing custom AE enchants: {total_missing}")
    print(f"{'='*60}")

    # Summary unique missing enchants
    all_missing = set()
    for fpath in file_refs:
        for key, orig_name, level in file_refs[fpath]:
            if key not in VANILLA_ENCHANTS and key not in CUSTOM_EFFECTS and key not in ae_enchants:
                all_missing.add(orig_name)
    if all_missing:
        print(f"\nUnique missing enchants: {', '.join(sorted(all_missing))}")
        print("(either add to enchantments.yml or remove from weapon/armor files)")

if __name__ == "__main__":
    main()
