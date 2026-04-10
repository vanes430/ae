package net.advancedplugins.ae.impl.effects.effects.effects.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import net.advancedplugins.ae.impl.effects.effects.actions.execution.ExecutionTask;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.PotionEffectMatcher;
import net.advancedplugins.ae.impl.utils.SchedulerUtils;
import net.advancedplugins.ae.impl.utils.nbt.utils.MinecraftVersion;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ApplyPotionEffect extends AdvancedEffect {
   private static int permanentLength = -1;
   private static final Map<UUID, List<PotionEffect>> activatedPermanentPotions = new HashMap<>();
   private static final Queue<Triple<Long, LivingEntity, Double>> healthQueue = new ArrayDeque<>();

   public ApplyPotionEffect(JavaPlugin plugin) {
      super(plugin, "POTION", "Add potion effect", "%e:<POTION>:<LEVEL>:[TICKS]");
      if (!MinecraftVersion.isNewerThan(MinecraftVersion.MC1_19_R3)) {
         permanentLength = Integer.MAX_VALUE;
      }
   }

   @Override
   public boolean executeEffect(ExecutionTask task, LivingEntity target, String[] args) {
      target.getScheduler().execute(ASManager.getInstance(), () -> executeEffectInternal(task, target, args), null, 1L);
      return true;
   }

   private boolean executeEffectInternal(ExecutionTask task, LivingEntity target, String[] args) {
      PotionEffectType potionEffectType = PotionEffectMatcher.matchPotion(args[0]);
      int level = ASManager.parseInt(args[1]);
      if (target == null) {
         return true;
      } else if (task.getBuilder().isPermanent()) {
         if (task.getBuilder().isRemoved()) {
            List<PotionEffect> list = activatedPermanentPotions.getOrDefault(target.getUniqueId(), new ArrayList<>());
            handlePermanentRemoval(target, list, potionEffectType, level);
            if (list.isEmpty()) {
               activatedPermanentPotions.remove(target.getUniqueId());
            } else {
               activatedPermanentPotions.put(target.getUniqueId(), list);
            }
         } else {
            PotionEffect potionEffect = new PotionEffect(potionEffectType, permanentLength, level, true, false);
            if (!target.hasPotionEffect(potionEffectType) || !ASManager.hasPotionEffect(target, potionEffectType, level)) {
               target.addPotionEffect(potionEffect);
            }

            List<PotionEffect> list = activatedPermanentPotions.getOrDefault(target.getUniqueId(), new ArrayList<>());
            list.add(potionEffect);
            activatedPermanentPotions.put(target.getUniqueId(), list);
         }

         return true;
      } else if (args.length <= 2) {
         return true;
      } else {
         int duration = ASManager.parseInt(args[2]);
         if (target.hasPotionEffect(potionEffectType) || ASManager.hasPotionEffect(target, potionEffectType, level)) {
            PotionEffect effect = target.getPotionEffect(potionEffectType);
            if (effect.getAmplifier() > level || effect.getAmplifier() == level && effect.getDuration() > duration) {
               return true;
            }
         }

         if (args.length > 3 && !ASManager.parseBoolean(args[3], true)) {
            return true;
         } else {
            target.addPotionEffect(new PotionEffect(potionEffectType, duration, level));
            return true;
         }
      }
   }

   public static void handlePermanentRemoval(LivingEntity target, List<PotionEffect> list, PotionEffectType potionEffectType, int level) {
      if (list.stream().<PotionEffectType>map(PotionEffect::getType).anyMatch(potionEffectType::equals)) {
         Iterator<PotionEffect> iterator = list.iterator();
         double hpBefore = target.getHealth();

         while (iterator.hasNext()) {
            PotionEffect potionEffect = iterator.next();
            if (potionEffect.getType().equals(potionEffectType) && potionEffect.getAmplifier() == level) {
               iterator.remove();
               break;
            }
         }

         healthQueue.add(Triple.of(System.currentTimeMillis(), target, hpBefore));
         target.removePotionEffect(potionEffectType);
      }
   }

   public static int getPermanentLength() {
      return permanentLength;
   }

   public static Map<UUID, List<PotionEffect>> getActivatedPermanentPotions() {
      return activatedPermanentPotions;
   }

   static {
      SchedulerUtils.runTaskTimer(() -> {
         long currentTime = System.currentTimeMillis();

         while (!healthQueue.isEmpty() && healthQueue.peek().getLeft() + 500L < currentTime) {
            Triple<Long, LivingEntity, Double> triple = healthQueue.poll();
            if (triple != null) {
               LivingEntity target = triple.getMiddle();
               double hpBefore = triple.getRight();
               if (!(hpBefore <= target.getHealth())) {
                  target.setHealth(Math.min(hpBefore, target.getMaxHealth()));
               }
            }
         }
      }, 1L, 1L);
   }
}
