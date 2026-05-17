package net.advancedplugins.ae.impl.effects.effects.mechanics.triggers.internal;

import net.advancedplugins.ae.impl.utils.FoliaScheduler;
import org.bukkit.inventory.EquipmentSlot;

class UserRepeaters {
   public final HashMap<EquipmentSlot, List<FoliaScheduler.Task>> itemRunnables = new HashMap<>();
}
