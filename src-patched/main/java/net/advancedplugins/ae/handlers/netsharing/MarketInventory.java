package net.advancedplugins.ae.handlers.netsharing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.advancedplugins.ae.Core;
import net.advancedplugins.ae.Values;
import net.advancedplugins.ae.enchanthandler.enchantments.AEnchants;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedEnchantment;
import net.advancedplugins.ae.handlers.commands.MCI;
import net.advancedplugins.ae.impl.utils.items.ItemBuilder;
import net.advancedplugins.ae.utils.AManager;
import net.advancedplugins.ae.utils.YamlFile;
import net.advancedplugins.ae.utils.fanciful.FancyMessage;
import net.advancedplugins.ae.utils.nbt.NBTapi;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class MarketInventory implements Listener {
   private static final LinkedHashMap<String, EnchantPreview> enchantHashmap = new LinkedHashMap<>();
   private static ScheduledTask marketTask = null;
   private static final List<UUID> pendingPlayersChats = new ArrayList<>();

   public static void init(JavaPlugin instance) {
      if (Values.m_aeMarket) {
         marketTask = Bukkit.getAsyncScheduler().runAtFixedRate(instance, (t) -> cache(), 0L, 12000L * 50L, TimeUnit.MILLISECONDS);
      }
   }

   public static void unload() {
      if (Values.m_aeMarket && marketTask != null) {
         marketTask.cancel();
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (e.getCurrentItem() != null) {
         if (e.getClickedInventory() != null) {
            if (!e.getCurrentItem().getType().equals(Material.AIR)) {
               if (e.getView().getTopInventory().getHolder() instanceof MarketInventory.MarketInvHolder) {
                  e.setCancelled(true);
                  Player p = (Player)e.getWhoClicked();
                  ItemStack i = e.getCurrentItem();
                  int slot = e.getRawSlot();
                  if (slot <= p.getOpenInventory().getTopInventory().getSize()) {
                     int page = 0;
                     Optional<ItemStack> contents = Arrays.stream(p.getOpenInventory().getTopInventory().getContents()).findFirst();
                     if (contents.isPresent()) {
                        page = Integer.parseInt(NBTapi.get("page", contents.get()));
                     }

                     if (NBTapi.contains("enchant", i)) {
                        String enchant = NBTapi.get("enchant", i);
                        if (e.getClick().equals(ClickType.RIGHT)) {
                           EnchantPreview ep = enchantHashmap.get(enchant);
                           p.closeInventory();
                           new FancyMessage("").link(ep.getCreatorProfile()).send(p);
                           new FancyMessage("").link(ep.getCreatorProfile()).send(p);
                           new FancyMessage(AManager.color("&cClick here to open Enchantment Author's profile")).link(ep.getCreatorProfile()).send(p);
                           return;
                        }

                        if (AEnchants.matchEnchant(enchant) != null) {
                           p.sendMessage("§c§lERROR:§c This enchantment already exists on your server!");
                           return;
                        }

                        p.closeInventory();
                        get(p, enchant);
                     } else if (slot == 48) {
                        open(p, page - 1);
                     } else if (slot == 49) {
                        p.closeInventory();
                     } else if (slot == 50) {
                        open(p, page + 1);
                     } else if (slot == 53) {
                        p.closeInventory();
                        p.sendMessage(" ");
                        p.sendMessage(" ");
                        p.sendMessage(" §cWrite §lEnchantment Name§c in §lChat§c which you want to §lShare");
                        p.sendMessage(" §7§o((inappropriate or toxic enchantments will be removed and your access to market will be revoked))");
                        pendingPlayersChats.add(p.getUniqueId());
                     }
                  }
               }
            }
         }
      }
   }

   private static void get(Player p, String enchantName) {
      FileConfiguration tempConfig = new YamlConfiguration();
      File tempFile = new File(Core.getInstance().getDataFolder(), ".temp" + System.currentTimeMillis() + ".yml");
      String enchant = readUrl("http://servers.advancedmarket.co/ae/enchantments/share.php?get=" + enchantName).get(1);

      for (String line : enchant.split("7n7")) {
         boolean descLine = line.contains(".description");
         boolean typeLine = line.contains(".type");
         line = line.replaceAll("7l7", "%");
         line = line.replaceAll("7u7", "_");
         line = line.replaceAll("7sp7", " ");
         line = line.replaceAll("7and7", "&");
         if (!descLine) {
            line = line.replaceAll("7nl7", "\n");
         } else {
            line = line.replaceAll("7nl7", "\\\\n");
         }

         String[] data = line.split("7p7");
         String path = data[0].replace("</br>", "");
         if (data.length < 2) {
            p.sendMessage(MCI.color("&c\"" + enchantName + "\" is an invalid enchantment! Please report it via our Support Discord. Thank you."));
            return;
         }

         String arg = data[1];
         if (typeLine) {
            arg = arg.replaceAll(" ", "_");
            tempConfig.set(path, arg);
         } else if (isInteger(arg)) {
            tempConfig.set(path, Integer.parseInt(arg));
         } else if (isList(arg)) {
            List<String> list = Arrays.asList(arg.replace("[", "").replace("]", "").replaceAll(Pattern.quote(",_"), ", ").split(Pattern.quote(", ")));
            tempConfig.set(path, list);
         } else if (isBoolean(arg)) {
            tempConfig.set(path, Boolean.parseBoolean(arg));
         } else {
            tempConfig.set(path, arg.replaceAll("_", " "));
         }
      }

      try {
         tempFile.createNewFile();
         tempConfig.save(tempFile);
      } catch (Exception var15) {
         var15.printStackTrace();
      }

      String txt = readTextFile("enchantments.yml") + readTextFile(tempFile.getName());
      writeTextFile("enchantments.yml", txt);
      tempFile.delete();
      p.sendMessage("§2§lx §aSuccessfully downloaded enchantment §l" + enchantName + "§a, reloading...");
      MCI.reload(p);
   }

   public static boolean isList(String input) {
      return input.startsWith("[") && input.endsWith("]");
   }

   public static boolean isBoolean(String input) {
      return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false");
   }

   public static boolean isInteger(String input) {
      try {
         Integer.parseInt(input);
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   private static void cache() {
      List<String> enchants;
      try {
         String url = "http://servers.advancedmarket.co/ae/enchantments/share.php?list=true";
         enchants = readUrl(url);
      } catch (Exception var15) {
         enchants = new ArrayList<>();
      }

      enchantHashmap.clear();
      HashMap<String, EnchantPreview> unsorted = new HashMap<>();

      for (String line : enchants) {
         try {
            line = line.replaceAll("</br>", "");
            String[] data = line.split(Pattern.quote(":"), 7);
            String enchName = data[0].replaceAll("_", " ");
            int popularity = Integer.parseInt(data[1]);
            String type = data[2];
            int lvl = Integer.parseInt(data[3]);
            String appliesTo = data[4].replaceAll("_", " ");
            String desc = data[5].replaceAll("_", " ");

            int creator;
            try {
               creator = Integer.parseInt(data[6]);
            } catch (Exception var13) {
               creator = 0;
            }

            unsorted.put(enchName, new EnchantPreview(enchName, desc, lvl, appliesTo, type, popularity, creator));
         } catch (Exception var14) {
         }
      }

      List<Entry<String, EnchantPreview>> entries = new ArrayList<>(unsorted.entrySet());
      Collections.reverse(entries);
      entries.sort((a, b) -> b.getValue().getDownloadCount().compareTo(a.getValue().getDownloadCount()));

      for (Entry<String, EnchantPreview> entry : entries) {
         enchantHashmap.put(entry.getKey(), entry.getValue());
      }
   }

   @EventHandler
   public void onChat(AsyncPlayerChatEvent e) {
      if (!e.isCancelled()) {
         if (e.getPlayer().isOp()) {
            if (pendingPlayersChats.contains(e.getPlayer().getUniqueId())) {
               String enchant = e.getMessage();
               AdvancedEnchantment ae = AEnchants.matchEnchant(enchant);
               Player p = e.getPlayer();
               pendingPlayersChats.remove(p.getUniqueId());
               e.setCancelled(true);
               if (ae == null) {
                  p.sendMessage("§cEnchantment cannot be found.");
               } else {
                  boolean shared = share(p, enchant);
                  if (shared) {
                     p.sendMessage("§aShared §l" + enchant + "§a to the market! §lThank you!");
                  }
               }
            }
         }
      }
   }

   protected static boolean share(CommandSender p, String enchantName) {
      ConfigurationSection cs = YamlFile.ENCHANTMENTS.getConfigSection(enchantName);
      StringBuilder enchant = new StringBuilder();

      for (String key : cs.getKeys(true)) {
         String path = enchantName + "." + key;
         if (!YamlFile.ENCHANTMENTS.contains(path)) {
            String obj = YamlFile.ENCHANTMENTS.get(path) + "";
            obj = obj.replaceAll("%", "7l7");
            obj = obj.replaceAll("_", "7u7");
            obj = obj.replaceAll(" ", "7sp7");
            obj = obj.replaceAll("&", "7and7");
            obj = obj.replaceAll("\n", "7nl7");
            enchant.append(path).append("7p7").append(obj).append("7n7");
         }
      }

      String enchantString = enchant.toString().replaceAll("\n", "7nl7").replaceAll("(\r\n|\n)", "7nl7").replaceAll(" ", "7sp7");
      String type = cs.getString("type");
      int levels = cs.getConfigurationSection("levels").getKeys(false).size();
      String appliesTo = cs.getString("applies-to");
      String desc = cs.getString("description").replaceAll("\n", "7nl7");
      String url = ("http://servers.advancedmarket.co/ae/enchantments/share.php?set="
            + enchantName
            + "&desc="
            + desc
            + "&type="
            + type
            + "&levels="
            + levels
            + "&appliesTo="
            + appliesTo
            + "&yml="
            + enchantString
            + "&creator=")
         .replaceAll(" ", "_");
      List<String> output = readUrl(url);
      int rt = Integer.parseInt(output.get(0));
      return react(p, rt);
   }

   private static boolean react(CommandSender p, int code) {
      switch (code) {
         case 1:
            p.sendMessage("§c§nERROR:§c This action is current on cooldown.");
            return false;
         case 2:
            p.sendMessage("§c§nERROR:§c This enchantment already exists.");
            return false;
         case 3:
            p.sendMessage("§a§LSUCCESS:§a You successfully completed this action.");
            return true;
         case 4:
            p.sendMessage("§c§nERROR:§c This action is currently inaccessible.");
            return false;
         default:
            p.sendMessage("§c§lFailed to connect to remote server.");
            return false;
      }
   }

   public static List<String> readUrl(String input) {
      List<String> rt = new ArrayList<>();

      try {
         URLConnection connection = new URL(input.replaceAll(" ", "_")).openConnection();
         connection.setRequestProperty(
            "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
         );
         connection.connect();
         BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

         String line;
         while ((line = r.readLine()) != null) {
            rt.add(line);
         }

         r.close();
         return rt;
      } catch (Exception var5) {
         return Collections.singletonList("4");
      }
   }

   public static void open(Player p, int page) {
      if (!Values.m_aeMarket) {
         p.sendMessage("§cFeature is disabled.");
      } else {
         try {
            Inventory inv = Bukkit.createInventory(new MarketInventory.MarketInvHolder(), 54, "§a§lEnchantment Market");
            if (page < 1) {
               p.sendMessage("§cPage §l" + page + "§c does not exist!");
               return;
            }

            int amountPerPage = 36;
            int startWith = (page - 1) * amountPerPage;
            int endWith = page * amountPerPage;
            if (enchantHashmap.size() < startWith) {
               p.sendMessage("§cPage §l" + page + "§c does not exist!");
               return;
            }

            if (enchantHashmap.size() < endWith && enchantHashmap.size() > startWith) {
               endWith = enchantHashmap.size();
            }

            for (String ench : enchantHashmap.isEmpty() ? new ArrayList() : new ArrayList<>(enchantHashmap.keySet()).subList(startWith, endWith)) {
               EnchantPreview enchantPreview = enchantHashmap.get(ench);
               ItemBuilder itemBuilder = new ItemBuilder(getItemStackDependingOnEnchantType(enchantPreview.getEnchantmentType()))
                  .setAmount(enchantPreview.getLevelCount())
                  .setName("§7Enchantment §6" + StringUtils.capitalize(ench.toLowerCase(Locale.ROOT)));
               String[] desc = enchantPreview.getDesc().contains("\n") ? enchantPreview.getDesc().split("\n") : enchantPreview.getDesc().split("7nl7");

               for (String line : desc) {
                  if (line.chars().count() > 48L) {
                     String[] words = line.split(" ");
                     int addTo = words.length / 2;
                     itemBuilder.addLoreLine("§e§lx §7" + collectArrayTogether(words, 0, addTo));
                     itemBuilder.addLoreLine("§e§lx §7" + collectArrayTogether(words, addTo, words.length - 1));
                  } else {
                     itemBuilder.addLoreLine("§e§lx §7" + line);
                  }
               }

               itemBuilder.addLoreLine("");
               itemBuilder.addLoreLine("§6§lx §7Popularity: §e" + enchantPreview.getDownloadCount() + " §eDownloads");
               itemBuilder.addLoreLine("");
               itemBuilder.addLoreLine("§6§lx §7Enchant Type: §e" + enchantPreview.getEnchantmentType());
               itemBuilder.addLoreLine("§6§lx §7Applies to: §e" + enchantPreview.getAppliesTo());
               itemBuilder.addLoreLine("§6§lx §7Levels: §e" + enchantPreview.getLevelCount());
               itemBuilder.addLoreLine(" ");
               itemBuilder.addLoreLine("§8>> §6Right-Click §7to §eVisit Author§7 on §6SpigotMc");
               itemBuilder.addLoreLine("§8>> §6Left-Click §7to §eDownload");
               ItemStack ting = itemBuilder.toItemStack();
               ting = NBTapi.addNBTTag("enchant", ench, ting);
               ting = NBTapi.addNBTTag("page", page + "", ting);
               inv.addItem(new ItemStack[]{ting});
            }

            inv.setItem(
               45,
               new ItemBuilder(Material.BOOK)
                  .setName("§6What is this?")
                  .setLore(
                     "",
                     "§eEnchantments Market §7- Way for community",
                     "§7to share cool enchantments between without",
                     "§7having to directly interact together.",
                     "§7",
                     "§8>> §7Click on any enchantment to download and install.",
                     "§8>> §7Click on the Minecart to upload any of your enchants.",
                     "",
                     "§cThe market §lupdates§c every §l10§c minutes."
                  )
                  .toItemStack()
            );

            for (int i : Arrays.asList(46, 47, 51, 52)) {
               inv.setItem(i, new ItemBuilder(AManager.matchMaterial("THIN_GLASS", 1, 0)).setName(" ").toItemStack());
            }

            inv.setItem(48, new ItemBuilder(AManager.matchMaterial("BOOK", 1, 0)).setName("§8§ §6Previous Page").toItemStack());
            inv.setItem(49, new ItemBuilder(Material.ANVIL, page).setName("§6Close the selector.").toItemStack());
            inv.setItem(50, new ItemBuilder(AManager.matchMaterial("BOOK", 1, 0)).setName("§8>> §6Next Page").setSkullOwner("MHF_ArrowRight").toItemStack());
            inv.setItem(
               53,
               new ItemBuilder(AManager.matchMaterial("STORAGE_MINECART", 1, 0))
                  .setName("§6Share an enchantment")
                  .setLore(
                     "",
                     "§e§lClick here to share an enchantment.",
                     "§7 You will be asked to write the name of",
                     "§7 enchant you want to share in chat.",
                     "",
                     "§c§lUpload rules:",
                     " §7#1 §cYou can only upload an enchant every 3 minutes",
                     " §7#2 §cYour enchant must have an unique name",
                     " §7#3 §cEnchantment must be tested & working",
                     " §7#4 §cNames must be appropriate along with description",
                     "§4§nAny abuse of these rules will invalid your market license"
                  )
                  .toItemStack()
            );
            p.openInventory(inv);
         } catch (Exception var17) {
            var17.printStackTrace();
            p.sendMessage("§c§lERROR:§c Please wait until the market is cached in your system.");
         }
      }
   }

   private static String collectArrayTogether(String[] arr, int startFrom, int endWith) {
      StringBuilder stringBuilder = new StringBuilder();

      for (int i = startFrom; i < endWith; i++) {
         stringBuilder.append(arr[i] + " ");
      }

      return stringBuilder.toString();
   }

   private static ItemStack getItemStackDependingOnEnchantType(String at) {
      return new ItemStack(Material.BOOK);
   }

   public static String readTextFile(String fileName) {
      StringBuilder returnValue = new StringBuilder();

      try {
         FileReader file = new FileReader(new File(Core.getInstance().getDataFolder(), fileName));

         String line;
         try (BufferedReader reader = new BufferedReader(file)) {
            while ((line = reader.readLine()) != null) {
               returnValue.append(line).append("\n");
            }
         }
      } catch (FileNotFoundException var9) {
         throw new RuntimeException("File not found");
      } catch (IOException var10) {
         throw new RuntimeException("IO Error occurred");
      }

      return returnValue.toString();
   }

   public static void writeTextFile(String fileName, String s) {
      try {
         FileWriter output = new FileWriter(new File(Core.getInstance().getDataFolder(), fileName));
         BufferedWriter writer = new BufferedWriter(output);
         writer.write(s);
         writer.close();
         output.close();
      } catch (IOException var4) {
         var4.printStackTrace();
      }
   }

   private static class MarketInvHolder implements InventoryHolder {
      @NotNull
      public Inventory getInventory() {
         return null;
      }
   }
}
