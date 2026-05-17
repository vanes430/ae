package net.advancedplugins.ae.impl.effects.effects.effects.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.effects.effects.actions.execution.ExecutionTask;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.ColorUtils;
import net.advancedplugins.ae.impl.utils.EntitySpawnUtils;
import net.advancedplugins.ae.impl.utils.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GuardEffect extends AdvancedEffect {
   private final Map<UUID, UUID> guards = new HashMap<>();
   private static GuardEffect guardEffect;

   public GuardEffect(JavaPlugin plugin) {
      super(plugin, "GUARD", "Spawn a mob guarding player", "%e:<ENTITY>:<SECONDS>:<AMOUNT:><NAME>:<SWITCH_ROLES>");
      this.addArgument(0, EntityType.class);
      this.addArgument(1, Integer.class);
      this.addArgument(2, Integer.class);
      this.addArgument(3, String.class);
      this.addArgument(4, Boolean.class);
      guardEffect = this;
   }

   @Override
   public boolean executeEffect(ExecutionTask task, LivingEntity target, String[] args) {
      String type = args[0].toUpperCase(Locale.ROOT);
      boolean isBaby = type.startsWith("BABY_");
      String typeCheck = isBaby ? type.substring("BABY_".length()) : type;
      if (!ASManager.isValidEnum(EntityType.class, typeCheck)) {
         EffectsHandler.getInstance().getLogger().warning("Tried to summon GUARD of invalid entity type: \"" + typeCheck + "\".");
         return false;
      } else {
         EntityType entityType = EntityType.valueOf(typeCheck);
         int liveTicks = 160;
         int amount = 1;
         if (args.length > 1 && MathUtils.isInteger(args[1])) {
            liveTicks = ASManager.parseInt(args[1]) * 20;
         }

         if (args.length > 2 && MathUtils.isInteger(args[2].trim())) {
            amount = ASManager.parseInt(args[2].trim());
         }

         boolean switchRoles = false;
         if (args.length > 4) {
            switchRoles = ASManager.parseBoolean(args[4], false);
         }

         LivingEntity otherEntity = this.getOtherEntity(target, task);
         LivingEntity finalTarget = target;
         if (switchRoles) {
            finalTarget = otherEntity;
            otherEntity = target;
         }

         String name = "";
         if (args.length > 3) {
            name = ColorUtils.format(args[3].replaceAll("%player name%", finalTarget.getName()));
         }

         boolean attack = !finalTarget.equals(otherEntity);
         this.summonGuard(entityType, isBaby, name, liveTicks, amount, otherEntity, attack);
         return true;
      }
   }

   public boolean isGuard(Entity entity) {
      return this.guards.containsKey(entity.getUniqueId());
   }

   public void steal(UUID oldOwner, UUID newOwner) {
      if (this.guards.containsValue(oldOwner)) {
         Map<UUID, UUID> newMap = new HashMap<>(this.guards);

         for (Entry<UUID, UUID> entry : this.getGuards().entrySet()) {
            if (Objects.equals(entry.getValue(), oldOwner)) {
               newMap.remove(entry.getKey(), oldOwner);
               newMap.put(entry.getKey(), newOwner);
               Entity entity = Bukkit.getEntity(entry.getKey());
               if (entity instanceof Creature) {
                  ((Creature)entity).setTarget(Bukkit.getPlayer(newOwner));
               }
            }
         }

         this.guards.putAll(newMap);
      }
   }

   public void summonGuard(EntityType guardType, boolean isBaby, String name, int liveTicks, int amount, LivingEntity target, boolean attack) {
      for (int i = 0; i < amount; i++) {
         this.summonGuard(guardType, isBaby, name, liveTicks, target, attack);
      }
   }

   public void summonGuard(EntityType guardType, boolean isBaby, String name, int liveTicks, LivingEntity target, boolean attack) {
      Entity entity = EntitySpawnUtils.spawnEntity(this.getPlugin(), target.getWorld(), target.getLocation(), guardType);
      if (name != null && !name.isEmpty()) {
         entity.setCustomName(name);
         entity.setCustomNameVisible(true);
      }

      if (entity instanceof LivingEntity le) {
         le.setCanPickupItems(false);
         le.setRemoveWhenFarAway(true);
         le.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, liveTicks, 1, false, true));
      }

      this.guards.put(entity.getUniqueId(), target.getUniqueId());
      if (entity instanceof Creature) {
         ((Creature)entity).setTarget(target);
      }

      if (isBaby && entity instanceof Ageable) {
         ((Ageable)entity).setBaby();
      }

      SchedulerUtils.runTaskLater(() -> {
         entity.remove();
         if (!entity.isDead() && entity instanceof LivingEntity) {
            ((LivingEntity)entity).damage(2.147483647E9);
         }
      }, (long)liveTicks);
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onExplode(ExplosionPrimeEvent e) {
      if (this.isGuard(e.getEntity())) {
         if (e.getEntity() instanceof LivingEntity entity) {
            entity.getActivePotionEffects().forEach(pe -> entity.removePotionEffect(pe.getType()));
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onGuardDeath(EntityDeathEvent e) {
      if (this.isGuard(e.getEntity())) {
         e.setDroppedExp(0);
         e.getDrops().clear();
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   private void entityTargetEvent(EntityTargetEvent e) {
      if (this.isGuard(e.getEntity())) {
         if (e.getTarget() instanceof LivingEntity target) {
            if (!target.getUniqueId().equals(this.guards.get(e.getEntity().getUniqueId()))) {
               e.setCancelled(true);
               LivingEntity closest = null;
               double distance = 32.0;

               for (Entity entity : e.getTarget().getNearbyEntities(distance, distance, distance)) {
                  if (entity instanceof LivingEntity && !entity.getUniqueId().equals(entity.getUniqueId())) {
                     double entitiesDistance = entity.getLocation().distance(target.getLocation());
                     if (entitiesDistance < distance) {
                        closest = (LivingEntity)entity;
                        distance = entitiesDistance;
                     }
                  }
               }

               if (closest != null) {
                  e.setTarget(closest);
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onSpawn(CreatureSpawnEvent e) {
      if (this.isGuard(e.getEntity())) {
         e.setCancelled(false);
      }
   }

   public Map<UUID, UUID> getGuards() {
      return this.guards;
   }

   public static GuardEffect getGuardEffect() {
      return guardEffect;
   }
}
