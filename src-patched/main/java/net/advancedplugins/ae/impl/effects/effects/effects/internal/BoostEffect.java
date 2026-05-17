package net.advancedplugins.ae.impl.effects.effects.effects.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.effects.effects.actions.execution.ExecutionTask;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class BoostEffect extends AdvancedEffect {
   private final List<UUID> ignoreFall = new ArrayList<>();

   public BoostEffect(JavaPlugin plugin) {
      super(plugin, "BOOST", "Boost entity up in air", "%e:[DIRECTION]:[AMOUNT]");
      this.addArgument(0, Integer.class);
   }

   @Override
   public boolean executeEffect(ExecutionTask task, LivingEntity target, String[] args) {
      SchedulerUtils.runTaskLater(() -> {
               try {
                  String dir = "UP";
                  double amount = 1.0;
                  if (args.length == 1) {
                     if (MathUtils.isDouble(args[0])) {
                        amount = Double.parseDouble(args[0]);
                     } else {
                        dir = args[0];
                     }
                  } else if (args.length != 0) {
                     dir = args[0];
                     if (!MathUtils.isDouble(args[1])) {
                        EffectsHandler.getInstance().getLogger().severe("'BOOST' effect was used with an invalid amount! Expected a number, got " + args[1]);
                        return;
                     }

                     amount = MathUtils.clamp(Double.parseDouble(args[1]), -20.0, 20.0);
                  }

                  Vector direction = target.getLocation().getDirection().normalize();
                  double number = amount / 10.0;
                  double x = direction.getX() * number;
                  double z = direction.getZ() * number;
                  String var14 = dir.toUpperCase(Locale.ROOT);
                  Vector boost;
                  switch (var14) {
                     case "UP":
                        boost = new Vector(0.0, number, 0.0);
                        break;
                     case "DOWN":
                        boost = new Vector(0.0, -number, 0.0);
                        break;
                     case "FORWARD":
                        boost = new Vector(x, 0.0, z);
                        break;
                     case "BACKWARD":
                        boost = new Vector(-x, 0.0, -z);
                        break;
                     case "LOOK":
                        Vector eyeDirection = target.getEyeLocation().getDirection().normalize();
                        double eX = eyeDirection.getX() * number;
                        double eZ = eyeDirection.getZ() * number;
                        double eY = eyeDirection.getY() * number;
                        boost = new Vector(eX, eY, eZ);
                        break;
                     default:
                        EffectsHandler.getInstance()
                           .getLogger()
                           .severe("'BOOST' effect used with incorrect direction! You can choose from 'UP', 'DOWN', 'FORWARD', and 'BACKWARD'.");
                        return;
                  }

                  if (ASManager.isExcessVelocity(boost)) {
                     EffectsHandler.getInstance().getLogger().severe("'BOOST' effect used with too much velocity! Please lower the effect value.");
                     return;
                  }

                  target.setVelocity(boost);
                  if (!this.ignoreFall.contains(target.getUniqueId())) {
                     this.ignoreFall.add(target.getUniqueId());
                  }
               } catch (Exception var23) {
                  var23.printStackTrace();
               }
            }, 1L);
      return true;
   }

   @EventHandler
   public void onFall(EntityDamageEvent e) {
      if (e.getEntity().getType().equals(EntityType.PLAYER)) {
         if (e.getCause().equals(DamageCause.FALL)) {
            UUID u = e.getEntity().getUniqueId();
            if (this.ignoreFall.contains(u)) {
               e.setCancelled(true);
               this.ignoreFall.remove(u);
            }
         }
      }
   }
}
