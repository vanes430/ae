package net.advancedplugins.ae.impl.effects.effects.mechanics.triggers.internal;

import net.advancedplugins.ae.impl.effects.effects.abilities.AdvancedAbility;
import net.advancedplugins.ae.impl.effects.effects.actions.utils.RollItemType;
import net.advancedplugins.ae.impl.utils.ASManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

class RepeatingRunnable implements Runnable {
    private final RepeatingTrigger parent;
    private final ItemStack item;
    private final LivingEntity finalEntity;
    private final RollItemType type;
    private final Event e;
    private final AdvancedAbility ability;
    private boolean cancelled = false;

    RepeatingRunnable(
        RepeatingTrigger parent,
        ItemStack item,
        LivingEntity finalEntity,
        RollItemType type,
        Event e,
        AdvancedAbility ability
    ) {
        this.parent = parent;
        this.item = item;
        this.finalEntity = finalEntity;
        this.type = type;
        this.e = e;
        this.ability = ability;
    }

    @Override
    public void run() {
        if (cancelled) return;
        if (!ASManager.itemStackEquals(this.item, this.finalEntity.getEquipment().getItem(this.type.getSlot()), false)) {
            this.cancel();
        } else {
            this.parent
                .executionBuilder()
                .setAttacker(this.finalEntity)
                .setAttackerMain(true)
                .setEvent(this.e)
                .setItemType(this.type)
                .setItem(this.item)
                .asRepeating()
                .only(new AdvancedAbility[]{this.ability})
                .buildAndExecute();
        }
    }

    public void cancel() {
        this.cancelled = true;
    }
}
