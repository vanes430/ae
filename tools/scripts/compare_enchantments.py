#!/usr/bin/env python3
"""
Compare original JAR's enchantments.yml with our override (resources/enchantments.yml).
Outputs 3 sections: missing, extra, and changed enchantments.
"""

import yaml, json, sys
from pathlib import Path
from zipfile import ZipFile
from difflib import unified_diff

JAR_PATH = Path("libs/AdvancedEnchantments-9.24.2.jar")
OVERRIDE_PATH = Path("resources/enchantments.yml")

IGNORE_KEYS = {"levels"}  # skip per-level detail in summary diff (too verbose)

def load_original():
    with ZipFile(JAR_PATH) as z:
        return yaml.safe_load(z.read("enchantments.yml"))

def load_override():
    return yaml.safe_load(open(OVERRIDE_PATH))

def deep_diff(name, orig_val, over_val, depth=0):
    """Compare two values recursively, return list of changed field paths."""
    changes = []
    indent = "  " * depth

    if isinstance(orig_val, dict) and isinstance(over_val, dict):
        all_keys = set(orig_val.keys()) | set(over_val.keys())
        for k in sorted(all_keys):
            if k in IGNORE_KEYS:
                if orig_val.get(k) != over_val.get(k):
                    changes.append((f"{name}.{k}", "CHANGED (per-level detail hidden)"))
                continue
            if k not in orig_val:
                changes.append((f"{name}.{k}", f"ADDED in override: {json.dumps(over_val[k])[:120]}"))
            elif k not in over_val:
                changes.append((f"{name}.{k}", f"REMOVED from override: was {json.dumps(orig_val[k])[:120]}"))
            else:
                sub = deep_diff(f"{name}.{k}", orig_val[k], over_val[k], depth+1)
                changes.extend(sub)
    elif isinstance(orig_val, list) and isinstance(over_val, list):
        if orig_val != over_val:
            o = json.dumps(orig_val)
            n = json.dumps(over_val)
            if len(o) < 200 and len(n) < 200:
                changes.append((name, f"CHANGED: {o} → {n}"))
            else:
                changes.append((name, "CHANGED (list, too long to show inline)"))
    else:
        if orig_val != over_val:
            changes.append((name, f"CHANGED: {json.dumps(orig_val)} → {json.dumps(over_val)}"))

    return changes

def fmt_desc(e):
    d = str(e.get("description", "?"))
    return d.replace("\n", " | ")

def print_enchant_table(items, source_dict):
    if not items:
        print("  (none)")
        return
    print(f"  {'Enchantment':<20s} {'Group':<14s} {'Type':<20s} {'Lvls':<5s} {'Applies-to':<22s} Description")
    print(f"  {'-'*20} {'-'*14} {'-'*20} {'-'*5} {'-'*22} {'-'*40}")
    for name in items:
        e = source_dict[name]
        g = e.get("group", "?")
        t = e.get("type", "?")
        a = e.get("applies-to", "?")
        lvls = len(e.get("levels", {}))
        d = fmt_desc(e)
        print(f"  {name:<20s} {g:<14s} {t:<20s} {lvls:<5d} {a:<22s} {d}")

def main():
    orig = load_original()
    over = load_override()

    orig_keys = set(orig.keys())
    over_keys = set(over.keys())

    only_orig = sorted(orig_keys - over_keys)
    only_over = sorted(over_keys - orig_keys)
    common = sorted(orig_keys & over_keys)

    # ── Section 1: Only in original (missing from override) ──
    print("=" * 72)
    print("SECTION 1 — In ORIGINAL JAR but MISSING from override")
    print("  = probably need to ADD to resources/enchantments.yml")
    print("=" * 72)
    print_enchant_table(only_orig, orig)

    # ── Section 2: Only in override (extra, not in original) ──
    print()
    print("=" * 72)
    print("SECTION 2 — In OVERRIDE but NOT in original JAR")
    print("  = custom/local additions, may need REMOVAL")
    print("=" * 72)
    print_enchant_table(only_over, over)

    # ── Section 3: In both but changed ──
    print()
    print("=" * 72)
    print("SECTION 3 — In BOTH but CHANGED (may need UPDATE)")
    print("=" * 72)
    changed_count = 0
    for name in common:
        changes = deep_diff(name, orig[name], over[name])
        if changes:
            changed_count += 1
            print(f"\n  --- {name} ---")
            for path, desc in changes:
                print(f"    {path}: {desc}")
    if changed_count == 0:
        print("  (none - all identical)")

    # ── Summary ──
    print()
    print("=" * 72)
    print("SUMMARY")
    print("=" * 72)
    print(f"  Original JAR enchantments:  {len(orig)}")
    print(f"  Override enchantments:      {len(over)}")
    print(f"  Only in original (missing): {len(only_orig)}")
    print(f"  Only in override (extra):   {len(only_over)}")
    print(f"  In both but changed:        {changed_count}")
    print(f"  In both and identical:      {len(common) - changed_count}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
