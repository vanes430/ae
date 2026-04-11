package net.advancedplugins.ae.features.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.advancedplugins.ae.Core;
import net.advancedplugins.ae.api.AEAPI;
import net.advancedplugins.ae.handlers.commands.MCI;
import net.advancedplugins.ae.impl.effects.effects.abilities.AdvancedAbility;
import net.advancedplugins.ae.impl.utils.MathUtils;
import net.advancedplugins.ae.impl.utils.VanillaEnchants;
import net.advancedplugins.ae.impl.utils.nbt.utils.MinecraftVersion;
import net.advancedplugins.ae.utils.AManager;
import net.advancedplugins.ae.utils.YamlFile;
import net.advancedplugins.ae.utils.nbt.NBTapi;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdvancedWeapon {
    private String weaponName;
    private String weaponPath;
    private final HashMap<String, AdvancedAbility> weaponEvents = new HashMap<>();
    private String material;
    private String name;
    private List<String> lore;
    private List<String> enchants;
    private List<String> itemFlags;
    private final YamlFile config;
    private String requireSet;
    private int customModelData;

    public AdvancedWeapon(YamlFile config) {
        this.config = config;
        this.initSet();
    }

    public void initSet() {
        this.weaponPath = this.config.getFile().getName().split(".yml")[0];
        this.weaponName = MCI.color(this.config.getString("name"));
        this.requireSet = this.config.getString("requireSet");
        this.material = this.config.getString("material");
        this.name = this.config.getString("name");
        this.customModelData = this.config.getInt("customModelData");
        this.lore = this.config.getStringList("lore");
        this.enchants = this.config.getStringList("enchants");
        this.itemFlags = this.config.getStringList("itemFlags");
        if (this.config.isConfigSection("events")) {
            for (String event : this.config.getKeys("events")) {
                int cooldown = this.config.getInt("events." + event + ".cooldown", 0);
                String cooldownMessage = this.config.getString("events." + event + ".cooldownMessage", null);
                int chance = this.config.getInt("events." + event + ".chance", 100);
                List<String> effects = this.config.getStringList("events." + event + ".effects");
                List<String> condition = this.config.getStringList("events." + event + ".conditions", null);
                AdvancedAbility ability = AdvancedAbility.builder()
                    .setName("armorset - " + this.weaponName + " " + event)
                    .setType(Collections.singletonList(event))
                    .setChance(chance)
                    .setCooldown(cooldown)
                    .setCooldownMessage(cooldownMessage)
                    .setEffects(effects);
                if (condition != null) {
                    ability.setConditions(condition);
                }

                this.weaponEvents.put(event, ability);
            }
        }
    }

    public AdvancedAbility getWeaponEvent(String event) {
        return this.weaponEvents.get(event);
    }

    public ItemStack getItem() {
        ItemStack item = AManager.matchMaterial(this.material, 1, 0);
        ItemMeta im = item.getItemMeta();
        List<String> newLore = new ArrayList<>();
        this.lore.forEach(s -> newLore.add(MCI.color(s)));
        if (im == null) {
            return null;
        } else {
            im.setLore(newLore);
            im.setDisplayName(MCI.color(this.getWeaponName()));
            if (MinecraftVersion.getVersionNumber() >= 1140) {
                im.setCustomModelData(this.customModelData);
            }

            if (this.itemFlags != null) {
                this.itemFlags.forEach(s -> im.addItemFlags(new ItemFlag[]{ItemFlag.valueOf(s)}));
            }

            // Apply attribute modifiers if defined in config
            if (this.config.isConfigSection("attributes")) {
                for (String attrName : this.config.getKeys("attributes")) {
                    String path = "attributes." + attrName;
                    double amount = this.config.getDouble(path + ".amount");
                    String operation = this.config.getString(path + ".operation", "ADD_NUMBER");
                    Attribute attribute = Attribute.valueOf(attrName);
                    if (attribute != null) {
                        AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(operation);
                        AttributeModifier modifier = new AttributeModifier(
                            "ae_" + attrName + "_" + this.weaponPath,
                            amount,
                            op
                        );
                        im.addAttributeModifier(attribute, modifier);
                    }
                }
            }

            item.setItemMeta(im);
            if (this.enchants != null) {
                for (String enchant : this.enchants) {
                    String[] info = enchant.split(":");
                    String ench = info[0].toUpperCase(Locale.ROOT);
                    String lvl = info[1];
                    int[] level;
                    if (lvl.contains("-")) {
                        int min = AManager.parseInt(lvl.split("-")[0]);
                        int max = AManager.parseInt(lvl.split("-")[1]);
                        level = new int[]{min, max};
                    } else {
                        int stat = AManager.parseInt(lvl);
                        level = new int[]{stat, stat};
                    }

                    int finallevel = level[0] == level[1] ? level[0] : MathUtils.randomBetween(level[0], level[1]);
                    Enchantment e = VanillaEnchants.displayNameToEnchant(ench, false);
                    if (e == null) {
                        try {
                            item = AEAPI.applyEnchant(ench, finallevel, item);
                        } catch (Exception var13) {
                            if (var13 instanceof NullPointerException) {
                                Core.getInstance().getLogger().info("Enchantment " + ench + " cannot be found.");
                            }
                        }
                    } else {
                        ItemMeta meta = item.getItemMeta();
                        meta.addEnchant(e, finallevel, true);
                        item.setItemMeta(meta);
                    }
                }
            }

            return NBTapi.addNBTTag("AdvancedWeapon", this.getWeaponPath(), item);
        }
    }

    public String getWeaponName() {
        return this.weaponName;
    }

    public String getWeaponPath() {
        return this.weaponPath;
    }

    public HashMap<String, AdvancedAbility> getWeaponEvents() {
        return this.weaponEvents;
    }

    public String getMaterial() {
        return this.material;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public String getRequireSet() {
        return this.requireSet;
    }
}
