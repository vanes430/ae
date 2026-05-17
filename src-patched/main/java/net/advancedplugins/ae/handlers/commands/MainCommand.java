package net.advancedplugins.ae.handlers.commands;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import net.advancedplugins.ae.Core;
import net.advancedplugins.ae.api.AEAPI;
import net.advancedplugins.ae.enchanthandler.enchantments.AEnchants;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedEnchantment;
import net.advancedplugins.ae.features.alchemist.AlchemistInventory;
import net.advancedplugins.ae.features.enchanter.Enchanter;
import net.advancedplugins.ae.features.gkits.Gapi;
import net.advancedplugins.ae.features.souls.SoulsAPI;
import net.advancedplugins.ae.features.tinkerer.TinkererInventory;
import net.advancedplugins.ae.features.usercommands.EnchantmentInfo;
import net.advancedplugins.ae.handlers.commands.editor.EditorCommand;
import net.advancedplugins.ae.handlers.netbackend.PremadeCommandHandler;
import net.advancedplugins.ae.handlers.tokenHandler.ObtainToken;
import net.advancedplugins.ae.impl.effects.armorutils.ArmorType;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.ColorUtils;
import net.advancedplugins.ae.impl.utils.MathUtils;
import net.advancedplugins.ae.impl.utils.pdc.PDCHandler;
import net.advancedplugins.ae.utils.AManager;
import net.advancedplugins.ae.utils.GuestPaste;
import net.advancedplugins.ae.utils.ItemInHand;
import net.advancedplugins.ae.utils.PlInfo;
import net.advancedplugins.ae.utils.YamlFile;
import net.advancedplugins.ae.utils.ZipUtils;
import net.advancedplugins.ae.utils.lang.Lang;
import net.advancedplugins.ae.utils.nbt.NBTapi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class MainCommand implements TabExecutor {
   public static String name = "";
   private List<String> noArgsList = null;

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String commandLabel, String[] args) {
      if (!name.equalsIgnoreCase("pluginLoaded")) {
         return false;
      } else {
         for (int i = 0; i < args.length; i++) {
            String[] preProcessedArgument = args[i].split("-");
            int amount = 0;
            if (preProcessedArgument.length > 1) {
               try {
                  if (!MathUtils.isInteger(preProcessedArgument[0]) || !MathUtils.isInteger(preProcessedArgument[0])) {
                     continue;
                  }

                  int x1 = Integer.parseInt(preProcessedArgument[0]);
                  int x2 = Integer.parseInt(preProcessedArgument[1]);
                  if (x1 != x2) {
                     amount = ThreadLocalRandom.current().nextInt(x1, x2 + 1);
                  }
               } catch (Exception var19) {
                  amount = args.length == 4 ? Integer.parseInt(args[3].replaceAll("[^0-9]", "")) : 1;
               }

               if (amount != 0) {
                  args[i] = amount + "";
               }
            }
         }

         if (args.length != 0) {
            String var20 = args[0].toLowerCase(Locale.ROOT);
            switch (var20) {
               case "editor":
                  if (!sender.hasPermission("ae.command.editor")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  EditorCommand.openForPlayer((Player)sender);
                  return true;
               case "effectlist":
                  MCI.convertEffects(sender);
                  return true;
               case "lastchanged":
                  MCI.lastChanged(sender);
                  return true;
               case "reload":
                  MCI.reload(sender);
                  return true;
               case "debug":
                  if (!sender.hasPermission("ae.command.debug")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  if (args.length < 2) {
                     sender.sendMessage("Invalid argument: enabled or disabled expected.");
                     return true;
                  }

                  boolean b = args[1].equalsIgnoreCase("enable");
                  sender.sendMessage(ColorUtils.format((b ? "&aEnabled" : "&cDisabled") + " debug logging."));
                  sender.sendMessage(ColorUtils.format("&cWarning: This will be broadcasting debug to all the players. "));
                  ASManager.debug = b;
                  EffectsHandler.setDebug(b);
                  return true;
               case "setsouls":
                  if (!sender.hasPermission("ae.setsouls")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  if (args.length < 2) {
                     Lang.sendMessage(sender, "commands.main.setsouls.invalid-usage");
                     return true;
                  }

                  Player p = (Player)sender;
                  ItemStack item = p.getItemInHand();
                  if (item == null) {
                     Lang.sendMessage(sender, "commands.main.setsouls.invalid-item");
                     return true;
                  }

                  item = SoulsAPI.setSouls(item, Integer.parseInt(args[1]));
                  p.setItemInHand(item);
                  Lang.sendMessage(sender, "commands.main.setsouls.success", "%amount%;" + args[1]);
                  return true;
               case "unenchant":
                  if (!sender.hasPermission("ae.unenchant")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  if (args.length < 2) {
                     Lang.sendMessage(sender, "commands.main.unenchant.invalid-usage");
                     return true;
                  }

                  AdvancedEnchantment ae = AEnchants.matchEnchant(args[1]);
                  if (ae == null) {
                     Lang.sendMessage(sender, "commands.main.unenchant.invalid-enchant", "%enchant%;" + args[1]);
                     return true;
                  }

                  Player p = (Player)sender;
                  ItemStack app = new ItemInHand(p).get();
                  if (app != null && !AManager.isAir(app)) {
                     if (!NBTapi.hasEnchantment(ae.getPath(), app)) {
                        Lang.sendMessage(sender, "commands.main.unenchant.does-not-have-enchant", "%enchant%;" + ae.getPath());
                        return true;
                     }

                     app = NBTapi.removeEnchantment(ae.getPath(), app);
                     p.setItemInHand(app);
                     Lang.sendMessage(sender, "commands.main.unenchant.success", "%enchant%;" + ae.getPath());
                     return true;
                  }

                  Lang.sendMessage(sender, "commands.main.unenchant.not-holding-item");
                  return true;
               case "pastetypes":
                  if (!sender.hasPermission("ae.command.pastetypes")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  StringBuilder types = new StringBuilder();
                  GuestPaste paste = new GuestPaste("Enchant Types", types.toString());
                  paste.paste();
                  Lang.sendMessage(sender, "commands.main.pastetypes", "%url%;" + paste.getPasteLink());
                  return true;
               case "pasteenchants":
                  if (!sender.hasPermission("ae.pasteenchants")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  StringBuilder text = new StringBuilder();

                  for (AdvancedEnchantment enchants : AEnchants.getEnchantTypeList()
                     .stream()
                     .sorted(Comparator.comparing(AdvancedEnchantment::getName))
                     .collect(Collectors.toList())) {
                     text.append(enchants.getName());
                     text.append("\nDescription: ").append(String.valueOf(enchants.getDescription()).replaceAll("\n", " "));
                     text.append("\nApplies to: ").append(enchants.getAppliesTo());
                     text.append("\nGroup: ").append(enchants.getGroupName());
                     text.append("\nMax Level: ").append(enchants.getHighestLevel());
                     text.append("\n\n");
                  }

                  System.out.println("| Enchant Name | Description | Applies To | Max Level |");
                  System.out.println("|--------------|-------------|------------|-----------|");

                  for (AdvancedEnchantment enchants : AEnchants.getEnchantTypeList()
                     .stream()
                     .sorted(Comparator.comparing(AdvancedEnchantment::getName))
                     .collect(Collectors.toList())) {
                     System.out
                        .printf(
                           "| %s | %s | %s | %d |\n",
                           enchants.getName(),
                           String.valueOf(enchants.getDescription()).replaceAll("\n", " "),
                           enchants.getAppliesTo(),
                           enchants.getHighestLevel()
                        );
                  }

                  GuestPaste paste = new GuestPaste("Enchantments List", text.toString());
                  paste.paste();
                  Lang.sendMessage(sender, "commands.main.pastetypes", "%url%;" + paste.getPasteLink());
                  return true;
               case "open":
                  if (!sender.hasPermission("ae.admin")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  if (args.length < 3) {
                     Lang.sendMessage(sender, "commands.invalid-usage", "%usage%;/ae open <player> <enchanter/tinkerer/alchemist>");
                     return true;
                  }

                  String inv = args[2];
                  Player p = Bukkit.getPlayer(args[1]);
                  if (p != null && p.isOnline()) {
                     switch (inv) {
                        case "enchanter":
                           Enchanter.openForPlayer(p);
                           return true;
                        case "tinkerer":
                           TinkererInventory.open(p);
                           return true;
                        case "alchemist":
                           AlchemistInventory.open(p);
                           return true;
                        default:
                           Lang.sendMessage(sender, "commands.main.open", "%menu%;" + inv, "%player%;" + p.getName());
                           return true;
                     }
                  }

                  Lang.sendMessage(sender, "commands.offline-player");
                  return true;
               case "plinfo":
                  if (!sender.hasPermission("ae.command.plinfo")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  boolean includePlugins = args.length <= 1 || !args[1].equalsIgnoreCase("skipPlugins");
                  Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
                     sender.sendMessage(ColorUtils.format("&6Creating info link, please wait."));
                     sender.sendMessage(ColorUtils.format("&e" + PlInfo.createInfoLink(includePlugins)));
                  });
                  return true;
               case "zip":
                  if (!sender.hasPermission("ae.command.zip")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
                     Date date = new Date();
                     SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy--HH-mm-ss");
                     String formattedDate = dateFormat.format(date);
                     File dataFolder = new File(Core.getInstance().getDataFolder().getAbsolutePath());
                     File outputFilePath = new File(dataFolder + File.separator + "ae-" + formattedDate + ".zip");

                     try {
                        if (outputFilePath.exists()) {
                           Lang.sendMessage(sender, "commands.main.zip.zip-exists");
                           return;
                        }

                        outputFilePath.createNewFile();
                        ZipUtils.zip(Arrays.asList(dataFolder.listFiles()), outputFilePath);
                        Lang.sendMessage(sender, "commands.main.zip.success", "%path%;" + outputFilePath.getPath());
                     } catch (IOException var7x) {
                        var7x.printStackTrace();
                        Lang.sendMessage(sender, "commands.main.zip.error");
                     }
                  });
                  return true;
               case "list":
                  if (args.length == 1) {
                     MCI.sendList(sender, 1);
                  } else {
                     if (!MathUtils.isInteger(args[1])) {
                        Lang.sendMessage(sender, "commands.not-a-number", "%number%;" + args[1]);
                        return true;
                     }

                     MCI.sendList(sender, Integer.parseInt(args[1]));
                  }

                  return true;
               case "dev":
                  return true;
            }
         }

         if (sender instanceof Player player) {
            if (args.length == 0) {
               MCI.sendHelp(player);
               return true;
            } else {
               String var26 = args[0].toLowerCase(Locale.ROOT);
               switch (var26) {
                  case "claim":
                     if (!sender.hasPermission("ae.command.claim")) {
                        Lang.sendMessage(sender, "commands.no-permission");
                        return true;
                     }

                     if (args.length < 2) {
                        Lang.sendMessage(sender, "commands.main.claim.invalid-usage");
                        return true;
                     }

                     String token = args[1];
                     ObtainToken.get((Player)sender, token);
                     return true;
                  case "tinkereritem":
                     MCI.giveTinkererItem(player, args);
                     return true;
                  case "market":
                     MCI.market(player);
                     return true;
                  case "giverandombook":
                     MCI.giveBookWithRandomChances(sender, args);
                     return true;
                  case "givebook":
                     MCI.giveBookWithChances(player, args);
                     return true;
                  case "listnbt":
                     if (!sender.hasPermission("ae.command.listnbt")) {
                        Lang.sendMessage(sender, "commands.no-permission");
                        return true;
                     }

                     sender.sendMessage(ASManager.color("&4------ Item Information ------"));
                     if (player.getItemInHand() != null) {
                        sender.sendMessage("ArmorType: " + ArmorType.matchType(player.getItemInHand().getType().name()));
                     }

                     player.getItemInHand()
                        .getItemMeta()
                        .getPersistentDataContainer()
                        .getKeys()
                        .forEach(
                           key -> sender.sendMessage(ASManager.color("&bPDC -> &f" + key.toString() + ": " + PDCHandler.get(player.getItemInHand(), key)))
                        );
                     Object custom = player.getItemInHand().getItemMeta().serialize().get("custom");
                     sender.sendMessage("custom: " + custom);
                     if (custom != null) {
                        try {
                           byte[] decodedBytes = Base64.getDecoder().decode(custom + "");
                           ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedBytes);
                           GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                           DataInputStream dataInputStream = new DataInputStream(gzipStream);
                           byte[] buffer = new byte[1024];
                           int length = dataInputStream.read(buffer);
                           String decompressedString = new String(buffer, 0, length, "UTF-8").replace("\t", ",");
                           System.out.println(decompressedString);

                           while (dataInputStream.available() > 0) {
                              try {
                                 int value = dataInputStream.readInt();
                                 System.out.println("Integer found: " + value);
                              } catch (IOException var17) {
                                 break;
                              }
                           }

                           dataInputStream.close();
                           gzipStream.close();
                           byteStream.close();
                        } catch (IOException var18) {
                           var18.printStackTrace();
                        }
                     }

                     return true;
                  case "enchant":
                     MCI.enchant(player, args);
                     return true;
                  case "unenchant":
                     MCI.unEnchant(player, args);
                     return true;
                  case "admin":
                     MCI.openAdminInventory(player, args);
                     return true;
                  case "givercbook":
                     MCI.giveRightClickBook(sender, args);
                     return true;
                  case "view":
                     String permission = YamlFile.COMMANDS.getString("enchantment-info.permission", "");
                     if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                        Lang.sendMessage(sender, "commands.no-permission");
                        return true;
                     }

                     if (args.length == 1) {
                        Lang.sendMessage(sender, "commands.invalid-usage", "%usage%;/ae view <enchantment>");
                        return true;
                     }

                     if (!AEAPI.isAnEnchantment(args[1])) {
                        Lang.sendMessage(sender, "commands.invalid-enchant", "%enchant%;" + args[1]);
                        return true;
                     }

                     new EnchantmentInfo(player, AEnchants.matchEnchant(args[1])).open();
                     return true;
                  case "info":
                     MCI.sendInfoAboutEnchantment(player, args);
                     return true;
                  case "-about":
                  case "about":
                     MCI.sendPluginInfo(player);
                     return true;
                  case "giveitem":
                     MCI.giveItem(player, args);
                     return true;
                  case "givegkit":
                     MCI.givePlayerGKit(player, args);
                     return true;
                  case "give":
                     if (!player.hasPermission("ae.give")) {
                        Lang.sendMessage(player, "commands.no-permission");
                        return true;
                     }

                     if (args.length <= 3) {
                        this.incorrectCommandUsage(sender);
                        return true;
                     }

                     MCI.giveRandomBook(player, args);
                     return true;
                  case "magicdust":
                     MCI.giveDust(sender, args);
                     return true;
                  case "premade":
                     if (!player.hasPermission("ae.premade")) {
                        Lang.sendMessage(player, "commands.no-permission");
                        return true;
                     }

                     PremadeCommandHandler.open(player);
                     return true;
                  case "greset":
                     if (args.length < 3) {
                        return true;
                     }

                     if (!player.hasPermission("ae.gkits")) {
                        Lang.sendMessage(player, "commands.no-permission");
                        return true;
                     }

                     if (!args[2].equalsIgnoreCase("*") && !Gapi.isAKit(args[2])) {
                        Lang.sendMessage(sender, "gkits.unknown-gkit", "%gkit%;" + args[2]);
                        return true;
                     }

                     Player t = Bukkit.getPlayer(args[1]);
                     if (t != null) {
                        Gapi.resetDelay(t, args[2]);
                        Lang.sendMessage(sender, "commands.main.greset.reset", "%gkit%;" + args[2], "%player%;" + t.getName());
                        return true;
                     }
               }

               String argZeroOnlyNumbers = args[0].replaceAll("[^0-9]", "");
               if (!argZeroOnlyNumbers.isEmpty() && this.isValidInteger(argZeroOnlyNumbers)) {
                  if (!sender.hasPermission("ae.command.plinfo")) {
                     Lang.sendMessage(sender, "commands.no-permission");
                     return true;
                  }

                  int page = Integer.parseInt(argZeroOnlyNumbers);
                  if (page == 1) {
                     MCI.sendHelp(sender);
                  } else if (page == 2) {
                     MCI.sendHelp2(sender);
                  } else {
                     MCI.sendHelp(sender);
                  }
               } else {
                  this.incorrectCommandUsage(sender);
               }

               return true;
            }
         } else if (args.length == 0) {
            MCI.sendHelp(sender);
            return true;
         } else {
            String var23 = args[0].toLowerCase(Locale.ROOT);
            switch (var23) {
               case "givegkit":
                  MCI.givePlayerGKit(sender, args);
                  return true;
               case "magicdust":
                  MCI.giveDust(sender, args);
                  return true;
               case "giveitem":
                  MCI.giveItem(sender, args);
                  return true;
               case "givebook":
                  MCI.giveBookWithChances(sender, args);
                  return true;
               case "give":
                  if (args.length <= 3) {
                     this.incorrectCommandUsage(sender);
                     return true;
                  }

                  MCI.giveRandomBook(sender, args);
                  return true;
               case "givercbook":
                  MCI.giveRightClickBook(sender, args);
                  return true;
               case "giverandombook":
                  MCI.giveBookWithRandomChances(sender, args);
                  return true;
               case "tinkereritem":
                  MCI.giveTinkererItem(sender, args);
                  return true;
               case "info":
                  MCI.sendInfoAboutEnchantment(sender, args);
                  return true;
               default:
                  if (args[0].equalsIgnoreCase("greset") && args.length >= 3) {
                     if (!sender.hasPermission("ae.command.greset")) {
                        Lang.sendMessage(sender, "commands.no-permission");
                        return true;
                     }

                     Player t = Bukkit.getPlayer(args[1]);
                     if (t == null || !t.isOnline()) {
                        Lang.sendMessage(sender, "commands.offline-player");
                        return true;
                     }

                     if (args[2].equalsIgnoreCase("*")) {
                        Gapi.resetAll(t);
                        Lang.sendMessage(sender, "commands.main.greset.reset-all");
                        return true;
                     }

                     if (!Gapi.isAKit(args[2])) {
                        Lang.sendMessage(sender, "gkits.unknown-gkit", "%gkit%;" + args[2]);
                     }

                     Gapi.resetDelay(t, args[2]);
                     Lang.sendMessage(sender, "commands.main.greset.reset", "%gkit%;" + args[2], "%player%;" + t.getName());
                  }

                  return true;
            }
         }
      }
   }

   private boolean isValidInteger(String str) {
      try {
         Integer.parseInt(str);
         return true;
      } catch (NumberFormatException var3) {
         return false;
      }
   }

   private void incorrectCommandUsage(CommandSender sender) {
      Lang.sendMessage(sender, "commands.main.unknown-command");
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
      List<String> argsList = new ArrayList<>();
      if (args.length == 1 && this.noArgsList == null) {
         argsList.add("about");
         if (sender.hasPermission("ae.enchant")) {
            argsList.add("enchant");
         }

         if (sender.hasPermission("ae.givercbook")) {
            argsList.add("give");
            argsList.add("givercbook");
         }

         if (sender.hasPermission("ae.givebook")) {
            argsList.add("givebook");
         }

         if (sender.hasPermission("ae.givegkit")) {
            argsList.add("givegkit");
         }

         if (sender.hasPermission("ae.giveitem")) {
            argsList.add("giveitem");
         }

         if (sender.hasPermission("ae.giverandombook")) {
            argsList.add("giverandombook");
         }

         if (sender.hasPermission("ae.gkits")) {
            argsList.add("greset");
         }

         if (sender.hasPermission("ae.info")) {
            argsList.add("info");
         }

         if (sender.hasPermission("ae.lastchanged")) {
            argsList.add("lastchanged");
         }

         if (sender.hasPermission("ae.list")) {
            argsList.add("list");
         }

         if (sender.hasPermission("ae.givedust")) {
            argsList.add("magicdust");
         }

         if (sender.hasPermission("ae.open")) {
            argsList.add("open");
         }

         if (sender.hasPermission("ae.pasteenchants")) {
            argsList.add("pasteenchants");
         }

         if (sender.hasPermission("ae.plinfo")) {
            argsList.add("plinfo");
         }

         if (sender.hasPermission("ae.premade")) {
            argsList.add("premade");
         }

         if (sender.hasPermission("ae.reload")) {
            argsList.add("reload");
         }

         if (sender.hasPermission("ae.setsouls")) {
            argsList.add("setsouls");
         }

         if (sender.hasPermission("ae.givetinkerer")) {
            argsList.add("tinkereritem");
         }

         if (sender.hasPermission("ae.unenchant")) {
            argsList.add("unenchant");
         }

         if (sender.hasPermission("ae.zip")) {
            argsList.add("zip");
         }
      }

      if (!sender.hasPermission("ae.admin")) {
         return argsList;
      } else if (args.length == 1) {
         if (this.noArgsList == null) {
            argsList.add("market");
            argsList.add("enchant");
            argsList.add("unenchant");
            argsList.add("debug");
            argsList.add("list");
            argsList.add("admin");
            argsList.add("giveitem");
            argsList.add("greset");
            argsList.add("tinkererItem");
            argsList.add("give");
            argsList.add("setSouls");
            argsList.add("info");
            argsList.add("reload");
            argsList.add("lastchanged");
            argsList.add("magicdust");
            argsList.add("givebook");
            argsList.add("givercbook");
            argsList.add("premade");
            argsList.add("giverandombook");
            argsList.add("pasteenchants");
            argsList.add("givegkit");
            argsList.add("open");
            argsList.add("zip");
            argsList.add("plinfo");
            argsList.add("view");
            Collections.sort(argsList);
            this.noArgsList = new ArrayList<>(argsList);
         }

         return (List<String>)StringUtil.copyPartialMatches(args[0], this.noArgsList, argsList);
      } else {
         List<String> allArgs = new ArrayList<>();
         String argToMatch = args[0].toLowerCase(Locale.ROOT);
         switch (argToMatch) {
            case "enchant":
               if (args.length == 2) {
                  AEnchants.getEnchantTypeList().forEach(e -> allArgs.add(e.getPath()));
               } else if (args.length == 3) {
                  AdvancedEnchantment ae = AEnchants.matchEnchant(args[1].toLowerCase(Locale.ROOT));
                  if (ae != null) {
                     for (int i = 1; i < ae.getHighestLevel() + 1; i++) {
                        allArgs.add(i + "");
                     }
                  }
               }
               break;
            case "unenchant":
               if (sender instanceof Player && args.length == 2) {
                  ItemInHand item = new ItemInHand((Player)sender);
                  AEAPI.getEnchantmentsOnItem(item.get()).forEach((e, l) -> allArgs.add(e));
               }
               break;
            case "debug":
               if (args.length == 2) {
                  allArgs.add("enable");
                  allArgs.add("disable");
               }
               break;
            case "giveitem":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  String[] items = ColorUtils.stripColor(
                        MCI.color(
                           "§oslotincreaser§7, whitescroll, mystery, §osecret§7, §omagic§7, blackscroll, §orandomizer§7, renametag, blocktrak, fishtrak, stattrak, soultracker, mobtrak, soulgem, transmog, holywhitescroll, orb"
                        )
                     )
                     .split(", ");
                  allArgs.addAll(Arrays.asList(items));
               } else if (args.length == 4) {
                  allArgs.add("1");
                  allArgs.add("64");
               } else if (args.length == 5
                  && !args[2].contains("soul")
                  && !args[2].equalsIgnoreCase("blackscroll")
                  && !args[2].equalsIgnoreCase("blocktrak")
                  && !args[2].equalsIgnoreCase("fishtrak")
                  && !args[2].equalsIgnoreCase("fishtrak")
                  && !args[2].equalsIgnoreCase("mobtrak")
                  && !args[2].equalsIgnoreCase("mystery")
                  && !args[2].equalsIgnoreCase("renametag")
                  && !args[2].equalsIgnoreCase("stattrak")
                  && !args[2].equalsIgnoreCase("transmog")
                  && !args[2].equalsIgnoreCase("whitescroll")) {
                  if (args[2].equalsIgnoreCase("orb")) {
                     allArgs.add("ARMOR");
                     allArgs.add("WEAPON");
                     allArgs.add("TOOL");
                  } else {
                     allArgs.addAll(AEAPI.getGroups());
                  }
               }
               break;
            case "tinkereritem":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  allArgs.add("1");
                  allArgs.add("64");
               }
               break;
            case "givebook":
            case "give":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  AEnchants.getEnchantTypeList().forEach(e -> allArgs.add(e.getPath()));
               } else if (args.length == 4) {
                  AdvancedEnchantment ae = AEnchants.matchEnchant(args[2].toLowerCase(Locale.ROOT));
                  if (ae != null) {
                     ae.getLevelList().forEach(level -> allArgs.add(level + ""));
                  }
               }
               break;
            case "giverandombook":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  allArgs.addAll(AEAPI.getGroups());
               } else if (args.length == 4) {
                  allArgs.add("1");
                  allArgs.add("64");
               }
               break;
            case "givercbook":
               if (args.length == 2) {
                  allArgs.addAll(AEAPI.getGroups());
               } else if (args.length == 3) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 4) {
                  allArgs.add("1");
                  allArgs.add("64");
               }
               break;
            case "givegkit":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  allArgs.addAll(AEAPI.getGKits());
               }
               break;
            case "view":
            case "info":
               AEnchants.getEnchantTypeList().forEach(e -> allArgs.add(e.getPath()));
               break;
            case "magicdust":
               if (args.length == 2) {
                  allArgs.addAll(AEAPI.getGroups());
               } else if (args.length == 3) {
                  allArgs.add("1");
                  allArgs.add("100");
               } else if (args.length == 4) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               }
               break;
            case "open":
               if (args.length == 2) {
                  allArgs.addAll(this.allOnlinePlayerNames());
               } else if (args.length == 3) {
                  allArgs.add("enchanter");
                  allArgs.add("tinkerer");
                  allArgs.add("alchemist");
               }
               break;
            case "plinfo":
               if (args.length == 2) {
                  allArgs.add("skipPlugins");
               }
         }

         int argToMatch = args.length == 0 ? 0 : args.length - 1;
         StringUtil.copyPartialMatches(args[argToMatch], allArgs, argsList);
         Collections.sort(argsList);
         return argsList;
      }
   }

   private List<String> allOnlinePlayerNames() {
      List<String> playerNames = new ArrayList<>();

      for (Player player : Bukkit.getOnlinePlayers()) {
         playerNames.add(player.getName());
      }

      return playerNames;
   }
}
