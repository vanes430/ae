package net.advancedplugins.ae.impl.effects.effects.actions.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.advancedplugins.ae.impl.effects.api.EffectsActivateEvent;
import net.advancedplugins.ae.impl.effects.api.EffectsActivatedEvent;
import net.advancedplugins.ae.impl.effects.armorutils.ArmorType;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.effects.effects.abilities.AdvancedAbility;
import net.advancedplugins.ae.impl.effects.effects.actions.ActionExecution;
import net.advancedplugins.ae.impl.effects.effects.actions.ActionExecutionBuilder;
import net.advancedplugins.ae.impl.effects.effects.actions.handlers.DamageHandler;
import net.advancedplugins.ae.impl.effects.effects.effects.AdvancedEffect;
import net.advancedplugins.ae.impl.effects.effects.mechanics.targets.TargetResults;
import net.advancedplugins.ae.impl.effects.effects.mechanics.triggers.internal.ArmorWearTrigger;
import net.advancedplugins.ae.impl.effects.effects.variables.DynamicVariable;
import net.advancedplugins.ae.impl.effects.effects.variables.Variables;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.LocalLocation;
import net.advancedplugins.ae.impl.utils.SchedulerUtils;
import net.advancedplugins.ae.impl.utils.hooks.HookPlugin;
import net.advancedplugins.ae.impl.utils.hooks.HooksHandler;
import net.advancedplugins.ae.impl.utils.hooks.plugins.MythicMobsHook;
import net.advancedplugins.ae.impl.utils.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ExecutionTask {
    private ActionExecution actionExecution;
    private ActionExecutionBuilder builder;
    private Location location;
    private AdvancedAbility ability;
    private boolean targetsAreDead = false;
    private boolean soulboundOnly = false;
    private boolean sets = false;
    private boolean wait = false;
    private int currentWait = 0;
    private TargetResults targetResults;
    private final DamageHandler damageHandler = new DamageHandler();

    public ExecutionTask(AdvancedAbility ability, ActionExecution actionExecution, ActionExecutionBuilder builder) {
        this.ability = ability;
        this.actionExecution = actionExecution;
        this.builder = builder;
        this.location = builder.getMain().getLocation();
    }

    public void init() {
        if (!this.soulboundOnly || !this.actionExecution.getAllEffects().stream().noneMatch(s -> s.equalsIgnoreCase("KEEP_ON_DEATH"))) {
            List<LivingEntity> entitiesToAddFromEvent = new ArrayList<>();
            if (!this.sets && Bukkit.isPrimaryThread()) {
                EffectsActivateEvent effectsActivateEvent = new EffectsActivateEvent(this.ability, this.builder.getMain(), this.builder.getOther(), this);
                Bukkit.getPluginManager().callEvent(effectsActivateEvent);
                if (this.ability.getEffects().isEmpty() || effectsActivateEvent.isCancelled()) {
                    return;
                }

                if (effectsActivateEvent.getOtherTargets() != null) {
                    entitiesToAddFromEvent.addAll(effectsActivateEvent.getOtherTargets());
                }
            }

            for (String effect : this.ability.getEffects()) {
                List<Location> targetLocations = new ArrayList<>();
                LinkedList<LivingEntity> targetEntities = new LinkedList<>();
                effect = this.actionExecution.parseVariables(effect);
                this.updateMain(targetEntities, targetLocations, effect);
                targetEntities.addAll(entitiesToAddFromEvent);
                if (effect.startsWith("WAIT")) {
                    this.wait = true;
                }

                if (this.targetResults.getEffect() != null) {
                    effect = this.actionExecution.parseVariables(this.targetResults.getEffect());
                }

                String[] baseEffectSplit = effect.split(":");
                if (effect.startsWith("WAIT")) {
                    this.currentWait = this.currentWait + ASManager.parseInt(baseEffectSplit[1], 0);
                } else if (!this.wait) {
                    if (!targetLocations.isEmpty()) {
                        for (Location loc : targetLocations) {
                            this.activate(effect, this.builder.getMain(), loc);
                        }
                    } else {
                        for (LivingEntity target : targetEntities) {
                            this.activate(effect, target, null);
                        }
                    }
                } else {
                    final List<Location> locsCopy = new ArrayList<>(targetLocations);
                    final LinkedList<LivingEntity> entitiesCopy = new LinkedList<>(targetEntities);
                    final String effectCopy = effect;
                    SchedulerUtils.runTaskLater(() -> {
                        if (!locsCopy.isEmpty()) {
                            for (Location loc : locsCopy) {
                                this.activate(effectCopy, this.builder.getMain(), loc);
                            }
                        } else {
                            for (LivingEntity target : entitiesCopy) {
                                this.activate(effectCopy, target, null);
                            }
                        }
                    }, this.currentWait);
                }
            }
        }
    }

    private void activate(String initialEffect, LivingEntity ent, Location loc) {
        ASManager.debug("§d[ExecutionTask] start of " + initialEffect + " parsing+");
        String effect = DynamicVariable.parseThroughCustomVariables(
            Variables.replaceVariables(initialEffect, this.builder.getAttacker(), this.builder.getVictim(), this.actionExecution)
        );
        effect = EffectsHandler.getVariablesHandler().parseEffectLine(effect, this.builder.getMain(), this);
        effect = EffectsHandler.getFunctionsHandler().parseEffectLine(effect, this.builder.getMain(), this);
        effect = EffectsHandler.getPointersHandler().parseEffectLine(effect, this.builder.getType(), this);
        if (!effect.isEmpty() && !effect.contains("$skip")) {
            String[] effectSplit = effect.replaceAll(" ", "").split(":");
            String[] arguments = Arrays.copyOfRange(effectSplit, 1, effectSplit.length);
            AdvancedEffect advancedEffect = EffectsHandler.getEffectStorage().getEffect(effectSplit[0].replaceAll(" ", ""));
            if (advancedEffect == null) {
                this.reportIssue(
                    effect,
                    "Failed to activate effects as advancedEffect is null or invalid: '" + effectSplit[0].replaceAll(" ", "") + "'",
                    initialEffect,
                    "trigger:" + this.builder.getType(),
                    "entity:" + this.builder.getMain().getName()
                );
            } else {
                if (advancedEffect.hasStringArgument()) {
                    effectSplit = effect.split(":");
                    arguments = Arrays.copyOfRange(effectSplit, 1, effectSplit.length);
                }

                if (advancedEffect.isBlockEffect() && loc == null && this.builder.getEvent() instanceof BlockBreakEvent) {
                    loc = this.builder.getBlock().getLocation();
                }

                if (this.builder.getMain() instanceof Player && !this.builder.getMain().isOp() && advancedEffect.isExemptFromAC()) {
                    EffectsHandler.getAntiCheatHooks().forEach((key, value) -> value.exempt((Player) this.builder.getMain()));
                }

                if (this.builder.getEvent() instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) this.builder.getEvent();
                    if (HooksHandler.isEnabled(HookPlugin.MYTHICMOBS) && MythicMobsHook.getIgnoreEnchantsMobs().contains(event.getDamager())) {
                        return;
                    }
                }

                ASManager.debug(
                    "§d§l(!) "
                        + effect
                        + " activating "
                        + (loc != null ? "location based " + new LocalLocation(loc).getEncode() : "entity based " + (ent != null ? ent.getType().name() : "null"))
                );

                try {
                    if (loc != null) {
                        if (advancedEffect.executeEffect(this, loc, arguments)) {
                            if (!Bukkit.isPrimaryThread()) {
                                SchedulerUtils.runTask(() -> {
                                    EffectsActivatedEvent effectsActivatedEvent = new EffectsActivatedEvent(this.ability, this.builder.getMain(), ent, this);
                                    Bukkit.getPluginManager().callEvent(effectsActivatedEvent);
                                });
                            } else {
                                EffectsActivatedEvent effectsActivatedEvent = new EffectsActivatedEvent(this.ability, this.builder.getMain(), ent, this);
                                Bukkit.getPluginManager().callEvent(effectsActivatedEvent);
                            }
                        } else {
                            ASManager.debug("§d[ExecutionTask] skipping entity-based effect without valid target: " + effect);
                        }
                    } else if (ent != null) {
                        if (advancedEffect.isBlockEffect()) {
                            ASManager.debug("§d[ExecutionTask] skipping location-based effect without location: " + effect);
                        } else if (!advancedEffect.executeEffect(this, ent, arguments)) {
                            ASManager.debug("§d[ExecutionTask] effect returned false for " + effect);
                        } else {
                            if (!Bukkit.isPrimaryThread()) {
                                SchedulerUtils.runTask(() -> {
                                    EffectsActivatedEvent effectsActivatedEvent = new EffectsActivatedEvent(this.ability, this.builder.getMain(), ent, this);
                                    Bukkit.getPluginManager().callEvent(effectsActivatedEvent);
                                });
                            } else {
                                EffectsActivatedEvent effectsActivatedEvent = new EffectsActivatedEvent(this.ability, this.builder.getMain(), ent, this);
                                Bukkit.getPluginManager().callEvent(effectsActivatedEvent);
                            }

                            if (this.getBuilder().isPermanent() && this.getBuilder().isRemoved()) {
                                SchedulerUtils.runTaskLater(
                                    () -> {
                                        if (ent != null && !ent.isDead() && ASManager.isOnline(ent)) {
                                            ArmorType.getArmorContents(ent)
                                                .forEach((armorType, itemStack) -> ArmorWearTrigger.getArmorWearTrigger().runCheck(ent, itemStack, armorType, false, false));
                                        }
                                    },
                                    2L
                                );
                            }
                        }
                    } else {
                        // Fallback: try with builder's main entity or victim
                        LivingEntity fallbackEntity = this.builder.getVictim() != null ? this.builder.getVictim() : this.builder.getMain();
                        if (fallbackEntity != null && !fallbackEntity.isDead() && ASManager.isOnline(fallbackEntity)) {
                            advancedEffect.executeEffect(this, fallbackEntity, arguments);
                        } else {
                            ASManager.debug("§d[ExecutionTask] skipping effect - no valid target for " + effect);
                        }
                    }
                } catch (Exception var9) {
                    var9.printStackTrace();
                    this.reportIssue(effect, "Error while activating effect");
                }
            }
        }
    }

    public void reportIssue(String effect, String... args) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("ae.admin")) {
                p.sendMessage(Text.modify("&4Failed to activate effect '&f" + effect + "'"));
                p.sendMessage(Text.modify("&cAdditional information: &7" + String.join(", ", args)));
            }
        }

        ASManager.getInstance().getLogger().warning(Text.modify("&4Failed to activate effect '&f" + effect + "'"));
        ASManager.getInstance().getLogger().warning(Text.modify("&cAdditional information: &7" + String.join(", ", args)));
    }

    private void updateMain(LinkedList<LivingEntity> targetEntities, List<Location> targetLocations, String effect) {
        targetEntities.clear();
        targetLocations.clear();
        this.targetResults = EffectsHandler.getTargetHandler().handleTargets(effect, this);
        if (this.targetResults.getTargetList() != null) {
            targetEntities.addAll(this.targetResults.getTargetList());
        }

        if (this.targetResults.getTargetLocations() != null) {
            targetLocations.addAll(this.targetResults.getTargetLocations());
        }

        targetLocations = ASManager.removeDuplicateLocations(targetLocations);
    }

    public ExecutionTask asSets(boolean sets) {
        this.sets = sets;
        return this;
    }

    public ExecutionTask setLocation(Location location) {
        this.location = location;
        return this;
    }

    public ExecutionTask soulboundOnly(boolean b) {
        this.soulboundOnly = b;
        return this;
    }

    public ActionExecution getActionExecution() {
        return this.actionExecution;
    }

    public ActionExecutionBuilder getBuilder() {
        return this.builder;
    }

    public AdvancedAbility getAbility() {
        return this.ability;
    }

    public DamageHandler getDamageHandler() {
        return this.damageHandler;
    }
}
