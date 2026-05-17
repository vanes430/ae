package net.advancedplugins.ae.features.tinkerer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import net.advancedplugins.ae.Core;
import net.advancedplugins.ae.api.AEAPI;
import net.advancedplugins.ae.api.TinkererTradeEvent;
import net.advancedplugins.ae.enchanthandler.enchantments.AEnchants;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedEnchantment;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedGroup;
import net.advancedplugins.ae.impl.utils.ColorUtils;
import net.advancedplugins.ae.impl.utils.nbt.utils.MinecraftVersion;
import net.advancedplugins.ae.impl.utils.text.Text;
import net.advancedplugins.ae.utils.AManager;
import net.advancedplugins.ae.utils.PlayAESound;
import net.advancedplugins.ae.utils.YamlFile;
import net.advancedplugins.ae.utils.lang.Lang;
import net.advancedplugins.ae.utils.nbt.NBTapi;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class TinkererInventory implements Listener {
   public static Inventory tinkerer = null;
   private static String inventoryTitle;
   private static ItemStack acceptTradeItem = null;
   private static ItemStack playerConfirmItem = null;
   private static ItemStack tinkererConfirmItem = null;
   private static int playerConfirmSlot;
   private static boolean tinkererConfirm;
   private static int tinkererConfirmSlot;
   private static int[] splitterSlots;
   private static int[] playerSlots;
   private static int[] tinkererSlots;
   private static final List<UUID> successfulTrades = new ArrayList<>();
   private static final Map<Integer, Integer> slots = new HashMap<>();
   private static final Map<UUID, Inventory> inventoryMap = new HashMap<>();

   private static Map<Integer, Integer> getSlot() {
      return slots;
   }

   private static boolean isTinkerer(int i) {
      return i < 54;
   }

   public static void init() {
      inventoryTitle = Text.modify(
         YamlFile.TINKERER.getString("inventory.name") == null ? "You                   Tinkerer" : YamlFile.TINKERER.getString("inventory.name")
      );
      Inventory inventory = Bukkit.createInventory(new TinkererInventory.TinkererInvHolder(), 54, Text.modify(inventoryTitle));
      playerConfirmSlot = YamlFile.TINKERER.getInt("inventory.items.player-confirm-trade-item.slot", 0);
      tinkererConfirm = YamlFile.TINKERER.getBoolean("inventory.items.tinkerer-confirm-trade-item.enabled", true);
      tinkererConfirmSlot = YamlFile.TINKERER.getInt("inventory.items.tinkerer-confirm-trade-item.slot", 8);
      splitterSlots = Arrays.stream(YamlFile.TINKERER.getString("general.settings.splitter-slots", "4, 13, 22, 31, 40, 49").split(", "))
         .mapToInt(Integer::parseInt)
         .toArray();
      playerSlots = Arrays.stream(
            YamlFile.TINKERER
               .getString("general.settings.player-slots", "1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48")
               .split(", ")
         )
         .mapToInt(Integer::parseInt)
         .toArray();
      tinkererSlots = Arrays.stream(
            YamlFile.TINKERER
               .getString("general.settings.tinkerer-slots", "5, 6, 7, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53")
               .split(", ")
         )
         .mapToInt(Integer::parseInt)
         .toArray();
      ItemStack splitter = AManager.matchMaterial(
         YamlFile.TINKERER.getString("inventory.items.splitter-item.type"), 1, (byte)YamlFile.TINKERER.getInt("inventory.items.splitter-item.id")
      );
      ItemMeta splitterMeta = splitter.getItemMeta();
      if (splitterMeta != null) {
         splitterMeta.setDisplayName(ColorUtils.format(YamlFile.TINKERER.getString("inventory.items.splitter-item.name")));
         List<String> strings = new ArrayList<>();

         for (String string : YamlFile.TINKERER.getStringList("inventory.items.splitter-item.lore")) {
            strings.add(ColorUtils.format(string));
         }

         splitterMeta.setLore(strings);
         if (MinecraftVersion.getVersionNumber() >= 1140 && YamlFile.TINKERER.contains("inventory.items.splitter-item.custom-model-data")) {
            splitterMeta.setCustomModelData(YamlFile.TINKERER.getInt("inventory.items.splitter-item.custom-model-data"));
         }

         splitter.setItemMeta(splitterMeta);
      }

      for (int i : splitterSlots) {
         inventory.setItem(i, splitter);
      }

      acceptTradeItem = AManager.matchMaterial(
         YamlFile.TINKERER.getString("inventory.items.accept-trade-item.type"), 1, (byte)YamlFile.TINKERER.getInt("inventory.items.accept-trade-item.id")
      );
      ItemMeta agreeMeta = acceptTradeItem.getItemMeta();
      if (agreeMeta != null) {
         agreeMeta.setDisplayName(ColorUtils.format(YamlFile.TINKERER.getString("inventory.items.accept-trade-item.name")));
         List<String> agreeLore = new ArrayList<>();

         for (String s : YamlFile.TINKERER.getStringList("inventory.items.accept-trade-item.lore")) {
            agreeLore.add(ColorUtils.format(s));
         }

         agreeMeta.setLore(agreeLore);
         if (MinecraftVersion.getVersionNumber() >= 1140 && YamlFile.TINKERER.contains("inventory.items.accept-trade-item.custom-model-data")) {
            agreeMeta.setCustomModelData(YamlFile.TINKERER.getInt("inventory.items.accept-trade-item.custom-model-data"));
         }

         acceptTradeItem.setItemMeta(agreeMeta);
      }

      inventory.setItem(playerConfirmSlot, acceptTradeItem);
      if (tinkererConfirm) {
         inventory.setItem(tinkererConfirmSlot, acceptTradeItem);
         playerConfirmItem = AManager.matchMaterial(
            YamlFile.TINKERER.getString("inventory.items.player-confirm-trade-item.type", "STAINED_GLASS_PANE"),
            1,
            (byte)YamlFile.TINKERER.getInt("inventory.items.player-confirm-trade-item.id", 5)
         );
         ItemMeta playerConfirmItemMeta = playerConfirmItem.getItemMeta();
         if (playerConfirmItemMeta != null) {
            playerConfirmItemMeta.setDisplayName(
               YamlFile.TINKERER.getString("inventory.items.player-confirm-trade-item.name", "&aClick to confirm the transaction")
            );
            playerConfirmItemMeta.setLore(YamlFile.TINKERER.getStringList("inventory.items.player-confirm-trade-item.lore", new ArrayList<>()));
            if (MinecraftVersion.getVersionNumber() >= 1140 && YamlFile.TINKERER.contains("inventory.items.player-confirm-trade-item.custom-model-data")) {
               playerConfirmItemMeta.setCustomModelData(YamlFile.TINKERER.getInt("inventory.items.player-confirm-trade-item.custom-model-data"));
            }

            playerConfirmItem.setItemMeta(playerConfirmItemMeta);
         }

         tinkererConfirmItem = AManager.matchMaterial(
            YamlFile.TINKERER.getString("inventory.items.tinkerer-confirm-trade-item.type", "STAINED_GLASS_PANE"),
            1,
            (byte)YamlFile.TINKERER.getInt("inventory.items.tinkerer-confirm-trade-item.id", 5)
         );
         ItemMeta tinkererConfirmAgreeMeta = tinkererConfirmItem.getItemMeta();
         if (tinkererConfirmAgreeMeta != null) {
            tinkererConfirmAgreeMeta.setDisplayName(YamlFile.TINKERER.getString("inventory.items.tinkerer-confirm-trade-item.name"));
            tinkererConfirmAgreeMeta.setLore(YamlFile.TINKERER.getStringList("inventory.items.tinkerer-confirm-trade-item.lore", new ArrayList<>()));
            if (MinecraftVersion.getVersionNumber() >= 1140 && YamlFile.TINKERER.contains("inventory.items.tinkerer-confirm-trade-item.custom-model-data")) {
               tinkererConfirmAgreeMeta.setCustomModelData(YamlFile.TINKERER.getInt("inventory.items.tinkerer-confirm-trade-item.custom-model-data"));
            }

            tinkererConfirmItem.setItemMeta(tinkererConfirmAgreeMeta);
         }
      }

      tinkerer = inventory;
   }

   public static void open(@NotNull Player player) {
      Inventory cloneInventory = AManager.cloneInventory(tinkerer, inventoryTitle);
      inventoryMap.put(player.getUniqueId(), cloneInventory);
      PlayAESound.playSound(player, YamlFile.TINKERER.getString("inventory.open-sound", "BLOCK_CHEST_OPEN"));
      player.openInventory(cloneInventory);
   }

   public static void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof TinkererInventory.TinkererInvHolder) {
         if (AManager.isValid(event.getCurrentItem())) {
            Player player = (Player)event.getWhoClicked();
            if (event.getInventory().equals(inventoryMap.get(player.getUniqueId()))) {
               event.setCancelled(true);
               ItemStack currentItem = event.getCurrentItem();
               if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.SHIFT_LEFT) {
                  player.updateInventory();
               } else {
                  Inventory topInventory = player.getOpenInventory().getTopInventory();
                  if (!tinkererConfirm || !isTinkererItem(event.getRawSlot())) {
                     if (!AEAPI.isCustomEnchantBook(currentItem) && !NBTapi.hasEnchantments(currentItem)) {
                        if (!tinkererConfirm || event.getSlot() != tinkererConfirmSlot) {
                           if (event.getCurrentItem().isSimilar(playerConfirmItem) && event.getSlot() == playerConfirmSlot) {
                              if (canAgree(topInventory)) {
                                 trade(player, topInventory);
                              }
                           } else if (event.getCurrentItem().isSimilar(acceptTradeItem) && event.getSlot() == playerConfirmSlot) {
                              if (canAgree(topInventory)) {
                                 if (YamlFile.TINKERER.getBoolean("general.settings.enabled-confirm-items", true)) {
                                    topInventory.setItem(playerConfirmSlot, playerConfirmItem);
                                    if (tinkererConfirm) {
                                       topInventory.setItem(tinkererConfirmSlot, tinkererConfirmItem);
                                    }

                                    if (playerConfirmItem == null) {
                                       trade(player, topInventory);
                                    }
                                 } else {
                                    topInventory.setItem(playerConfirmSlot, playerConfirmItem);
                                    if (tinkererConfirm) {
                                       topInventory.setItem(tinkererConfirmSlot, tinkererConfirmItem);
                                    }

                                    trade(player, topInventory);
                                 }
                              }
                           } else if (!isPlaceholderItem(event.getRawSlot())) {
                              if (event.getCurrentItem() != null) {
                                 Lang.sendMessage(player, "tinkerer.non-tradeable-item");
                              }

                              player.updateInventory();
                           }
                        }
                     } else {
                        if (isTinkerer(event.getRawSlot())) {
                           int slot = getSlot().get(event.getRawSlot());
                           topInventory.setItem(slot, new ItemStack(Material.AIR));
                           event.setCurrentItem(new ItemStack(Material.AIR));
                           AManager.giveItem(player, currentItem);
                           if (!canAgree(topInventory)) {
                              if (topInventory.getItem(playerConfirmSlot).isSimilar(playerConfirmItem)) {
                                 topInventory.setItem(playerConfirmSlot, acceptTradeItem);
                              }

                              if (tinkererConfirm && topInventory.getItem(tinkererConfirmSlot).isSimilar(tinkererConfirmItem)) {
                                 topInventory.setItem(tinkererConfirmSlot, acceptTradeItem);
                              }
                           }

                           player.updateInventory();
                        } else {
                           if (topInventory.firstEmpty() == -1) {
                              return;
                           }

                           int playerSlot = getFirstEmptySlot(topInventory, playerSlots);
                           int tinkererSlot = getFirstEmptySlot(topInventory, tinkererSlots);
                           if (playerSlot == -1) {
                              return;
                           }

                           if (AEAPI.isCustomEnchantBook(currentItem)) {
                              if (currentItem.getAmount() > 1) {
                                 currentItem.setAmount(currentItem.getAmount() - 1);
                                 event.setCurrentItem(currentItem);
                              } else {
                                 event.setCurrentItem(new ItemStack(Material.AIR));
                              }

                              currentItem.setAmount(1);
                              topInventory.setItem(playerSlot, currentItem);
                              if (!AEAPI.isCustomEnchantBook(currentItem)) {
                                 return;
                              }

                              AdvancedEnchantment advancedEnchantment = AEnchants.matchEnchant(currentItem);
                              AdvancedGroup advancedGroup = advancedEnchantment.getGroup();
                              if (YamlFile.TINKERER.getBoolean("general.give-item-instead-of-dust.enabled", false)) {
                                 int amount = YamlFile.TINKERER.getInt("general.give-item-instead-of-dust.amountPerGroup." + advancedGroup.getName(), 100);
                                 topInventory.setItem(tinkererSlot, TinkererItems.getOutputItem(player, amount));
                              } else {
                                 topInventory.setItem(tinkererSlot, TinkererItems.secretDust(advancedGroup));
                              }

                              getSlot().put(playerSlot, tinkererSlot);
                           } else {
                              if (TinkererFormula.getAmountForItem(currentItem) == 0) {
                                 return;
                              }

                              if (!Arrays.stream(playerSlots).findFirst().isPresent()) {
                                 AManager.reportIssue(new NullPointerException(), "Player slots map is empty.");
                                 return;
                              }

                              topInventory.setItem(playerSlot, currentItem);
                              topInventory.setItem(tinkererSlot, TinkererItems.output(player, currentItem));
                              getSlot().put(playerSlot, tinkererSlot);
                              event.setCurrentItem(new ItemStack(Material.AIR));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void trade(Player player, Inventory inventory) {
      List<ItemStack> newItems = new ArrayList<>();

      for (int slot : tinkererSlots) {
         if (inventory.getItem(slot) != null) {
            newItems.add(inventory.getItem(slot));
         }
      }

      newItems.forEach(newItem -> AManager.giveItem(player, newItem));
      successfulTrades.add(player.getUniqueId());
      Lang.sendMessage(player, "tinkerer.trade-accepted");
      PlayAESound.playSound(player, YamlFile.TINKERER.getString("inventory.items.player-confirm-trade-item.sound", "BLOCK_LEVER_CLICK"));
      TinkererTradeEvent tinkererTradeEvent = new TinkererTradeEvent(player, newItems);
      Bukkit.getPluginManager().callEvent(tinkererTradeEvent);
      player.updateInventory();
      player.closeInventory();
   }

   private static boolean canAgree(Inventory inventory) {
      boolean canAgree = false;

      for (int slot : tinkererSlots) {
         if (inventory.getItem(slot) != null) {
            canAgree = true;
         }
      }

      return canAgree;
   }

   private static boolean isPlaceholderItem(int slot) {
      return IntStream.of(splitterSlots).anyMatch(x -> x == slot);
   }

   private static boolean isTinkererItem(int slot) {
      return IntStream.of(tinkererSlots).anyMatch(x -> x == slot);
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent e) {
      Player player = (Player)e.getPlayer();
      if (e.getInventory().equals(inventoryMap.get(player.getUniqueId()))) {
         inventoryMap.remove(player.getUniqueId());
         player.updateInventory();
         if (successfulTrades.contains(player.getUniqueId())) {
            successfulTrades.remove(player.getUniqueId());
            player.updateInventory();
         } else {
            Bukkit.getGlobalRegionScheduler().runDelayed(Core.getInstance(), (task) -> {
               if (player.isOnline() && player.isValid()) {
                  for (int i : playerSlots) {
                     ItemStack item = e.getInventory().getItem(i);
                     if (item != null) {
                        AManager.giveItem(player, item);
                     }
                  }
               }
            }, 1L);
         }
      }
   }

   private static int getFirstEmptySlot(Inventory inventory, int[] slots) {
      if (slots.length == 0) {
         return -1;
      } else {
         for (int slot : slots) {
            if (inventory.getItem(slot) == null) {
               return slot;
            }
         }

         return -1;
      }
   }

   public static class TinkererInvHolder implements InventoryHolder {
      @NotNull
      public Inventory getInventory() {
         return null;
      }
   }
}
