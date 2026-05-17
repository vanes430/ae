package net.advancedplugins.ae.impl.effects.effects.effects.internal;

import net.advancedplugins.ae.impl.effects.effects.actions.execution.ExecutionTask;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.utils.SchedulerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtinguishEffect extends AdvancedEffect {
   public ExtinguishEffect(JavaPlugin plugin) {
      super(plugin, "EXTINGUISH", "Extinguish an entity", "%e");
   }

   @Override
   public boolean executeEffect(ExecutionTask task, LivingEntity target, String[] args) {
      SchedulerUtils.runTaskLater(() -> target.setFireTicks(0), 1L);
      return true;
   }
}
