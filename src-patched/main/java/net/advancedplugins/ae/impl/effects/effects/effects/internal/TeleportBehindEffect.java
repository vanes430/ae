package net.advancedplugins.ae.impl.effects.effects.effects.internal;

import net.advancedplugins.ae.impl.effects.effects.actions.execution.ExecutionTask;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.utils.ASManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportBehindEffect extends AdvancedEffect {
   public TeleportBehindEffect(JavaPlugin plugin) {
      super(plugin, "TELEPORT_BEHIND", "Teleport behind other entity", "%e");
   }

   @Override
   public boolean executeEffect(ExecutionTask task, LivingEntity target, String[] args) {
      LivingEntity nonTarget = this.getOtherEntity(target, task);
      Location from = nonTarget.getLocation();
      float nang = from.getYaw() + 90.0F;
      if (nang < 0.0F) {
         nang += 360.0F;
      }

      double nX = Math.cos(Math.toRadians(nang));
      double nZ = Math.sin(Math.toRadians(nang));
      Location to = new Location(from.getWorld(), from.getX() - nX, from.getY(), from.getZ() - nZ, from.getYaw(), from.getPitch());
      if (!ASManager.isAir(to.getBlock())) {
         return true;
      } else {
         SchedulerUtils.runTask(() -> target.teleport(to, TeleportCause.UNKNOWN));
         return true;
      }
   }
}
