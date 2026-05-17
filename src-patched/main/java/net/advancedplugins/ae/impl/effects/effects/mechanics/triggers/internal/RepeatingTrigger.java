package net.advancedplugins.ae.impl.effects.effects.mechanics.triggers.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.effects.effects.abilities.AdvancedAbility;
import net.advancedplugins.ae.impl.effects.effects.actions.ActionExecution;
import net.advancedplugins.ae.impl.effects.effects.actions.AdvancedTrigger;
import net.advancedplugins.ae.impl.effects.effects.actions.utils.RollItemType;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RepeatingTrigger extends AdvancedTrigger {
   private static final HashMap<UUID, UserRepeaters> repeaters = new HashMap<>();
   public static final Cache<UUID, Map<String, Long>> COOLDOWN_QUEUE = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();
   private static RepeatingTrigger trigger;

   public RepeatingTrigger() {
      super("REPEATING");
      this.setDescription("Repeating activation of effects");
      trigger = this;
   }

   public void activate(LivingEntity ent, RollItemType type, ItemStack item, Event e) {
      if (this.isEnabled()) {
         EffectsHandler.debug("[REPEATING] Starting check for: " + ent.getType());
         if (item != null) {
            ActionExecution execution = this.executionBuilder()
               .setAttacker(ent)
               .setAttackerMain(true)
               .setEvent(e)
               .setItemType(type)
               .setItem(item)
               .asRepeating()
               .setSkipCooldown(true)
               .skipChances()
               .skipConditions()
               .build();
            execution.build();
            EffectsHandler.debug("[REPEATING] Found effects for " + ent.getType() + " " + item.getType() + ": " + execution.getEffects());
            if (!execution.getEffects().isEmpty()) {
               AdvancedAbility ability = execution.getEffects().getFirst();
               if (ability.getTypes().contains(this.getTriggerName())) {
                  EffectsHandler.debug("Adding repeating item: " + item.getType() + " " + ability.getName() + ": s" + ability.getRepeatingDelay());
                  this.addRepeatingItem(ent.getUniqueId(), type, execution.getEffects(), e, item);
               }
            }
         }
      }
   }

   public void stopAll(LivingEntity ent) {
      UserRepeaters r = getRepeaters().get(ent.getUniqueId());
      if (r != null) {
         r.itemRunnables.values().stream().flatMap(Collection::stream).forEach(FoliaScheduler.Task::cancel);
         r.itemRunnables.clear();
      }
   }

   public void stop(LivingEntity ent, RollItemType type, Event e) {
      UserRepeaters r = getRepeaters().get(ent.getUniqueId());
      if (r != null) {
         List<FoliaScheduler.Task> tasks = r.itemRunnables.get(type.getSlot());
         if (tasks != null && !tasks.isEmpty()) {
            tasks.forEach(FoliaScheduler.Task::cancel);
            tasks.clear();
            r.itemRunnables.remove(type.getSlot());
         }
      }
   }

   private void addRepeatingItem(UUID user, final RollItemType type, LinkedList<AdvancedAbility> effects, final Event e, final ItemStack item) {
      try {
         UserRepeaters r = getRepeaters().computeIfAbsent(user, s -> new UserRepeaters());
         Entity entity = Bukkit.getPlayer(user);
         if (entity == null) {
            entity = Bukkit.getEntity(user);
         }

         if (entity instanceof LivingEntity finalEntity) {
            if (finalEntity.getEquipment() != null) {
               Map<String, Long> cooldowns = (Map<String, Long>)COOLDOWN_QUEUE.getIfPresent(user);
               List<FoliaScheduler.Task> runnables = new ArrayList<>();

               for (final AdvancedAbility ability : effects) {
                  Long remainingCooldown = null;
                  if (cooldowns != null && cooldowns.containsKey(ability.getNameNoLevel())) {
                     if (cooldowns.get(ability.getNameNoLevel()) <= System.currentTimeMillis()) {
                        cooldowns.remove(ability.getNameNoLevel());
                     } else {
                        remainingCooldown = (cooldowns.get(ability.getNameNoLevel()) - System.currentTimeMillis()) / 50L;
                     }
                  }

                  if (cooldowns == null || !cooldowns.containsKey(ability.getNameNoLevel())) {
                     ((Map)COOLDOWN_QUEUE.get(user, HashMap::new))
                        .put(ability.getNameNoLevel(), System.currentTimeMillis() + ability.getRepeatingDelay() * 1000L);
                  }

                  long delay = remainingCooldown != null ? remainingCooldown : (ability.isRepeatingInstantApply() ? 0L : ability.getRepeatingDelay() * 20L);
                  FoliaScheduler.Task runnable = FoliaScheduler.runTaskTimer(EffectsHandler.getInstance(), () -> {
                           if (!ASManager.itemStackEquals(item, finalEntity.getEquipment().getItem(type.getSlot()), false)) {
                              UserRepeaters repeaters = getRepeaters().get(user);
                              if (repeaters != null && repeaters.itemRunnables.get(type.getSlot()) != null) {
                                 repeaters.itemRunnables.get(type.getSlot()).forEach(FoliaScheduler.Task::cancel);
                              }
                           } else {
                              RepeatingTrigger.this.executionBuilder()
                                 .setAttacker(finalEntity)
                                 .setAttackerMain(true)
                                 .setEvent(e)
                                 .setItemType(type)
                                 .setItem(item)
                                 .asRepeating()
                                 .only(ability)
                                 .buildAndExecute();
                           }
                        }, delay, ability.getRepeatingDelay() * 20L);
                  runnables.add(runnable);
               }

               r.itemRunnables.put(type.getSlot(), runnables);
            }
         }
      } catch (Throwable var17) {
         throw var17;
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent e) {
      this.stopAll(e.getPlayer());
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onWorldChange(PlayerChangedWorldEvent event) {
      this.stopAll(event.getPlayer());
   }

   @EventHandler(ignoreCancelled = true)
   public void onItemChange(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
      ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
      RollItemType oldType = RollItemType.getHand(player, oldItem);
      RollItemType newType = RollItemType.getHand(player, newItem);
      if (oldType != null) {
         this.stop(player, oldType, event);
      }

      if (newType != null) {
         this.activate(player, newType, newItem, event);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onItemDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItemDrop().getItemStack();
      RollItemType type = RollItemType.getHand(player, item);
      this.stop(player, type, event);
   }

   @EventHandler(ignoreCancelled = true)
   public void onItemPickUp(PlayerPickupItemEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem().getItemStack();
      RollItemType type = RollItemType.getHand(player, item);
      if (type != null) {
         this.activate(player, type, item, event);
      }
   }

   public static HashMap<UUID, UserRepeaters> getRepeaters() {
      return repeaters;
   }

   public static RepeatingTrigger getTrigger() {
      return trigger;
   }
}
