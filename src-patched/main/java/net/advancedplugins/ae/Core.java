package net.advancedplugins.ae;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.advancedplugins.ae.abilities.ItemEnchantsReader;
import net.advancedplugins.ae.enchanthandler.enchantments.AEnchantmentSorting;
import net.advancedplugins.ae.enchanthandler.enchantments.AEnchants;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedGroup;
import net.advancedplugins.ae.enchanthandler.enchantments.LocalAPI;
import net.advancedplugins.ae.enchanthandler.enchantments.books.BookRightClick;
import net.advancedplugins.ae.enchantmentTable.ETableHandler;
import net.advancedplugins.ae.features.alchemist.AlchemistInventory;
import net.advancedplugins.ae.features.alchemist.AlchemistInventoryClicks;
import net.advancedplugins.ae.features.enchanter.ConfirmInventory;
import net.advancedplugins.ae.features.enchanter.InteractEvent;
import net.advancedplugins.ae.features.enchanter.InventoryEvent;
import net.advancedplugins.ae.features.enchanter.PaymentHandler;
import net.advancedplugins.ae.features.enchanter.paymentMethods.DiamondsPayment;
import net.advancedplugins.ae.features.enchanter.paymentMethods.EmeraldsPayment;
import net.advancedplugins.ae.features.enchanter.paymentMethods.GoldIngotsPayment;
import net.advancedplugins.ae.features.enchanter.paymentMethods.LevelsPayment;
import net.advancedplugins.ae.features.gkits.GClick;
import net.advancedplugins.ae.features.gkits.GKits;
import net.advancedplugins.ae.features.gkits.editor.GListeners;
import net.advancedplugins.ae.features.mobDrops.MobDrops;
import net.advancedplugins.ae.features.orbs.Orb;
import net.advancedplugins.ae.features.orbs.OrbApply;
import net.advancedplugins.ae.features.sets.SetsManager;
import net.advancedplugins.ae.features.sets.commands.SetsCommand;
import net.advancedplugins.ae.features.sets.listeners.SetEquipEvent;
import net.advancedplugins.ae.features.sets.listeners.SkullPlace;
import net.advancedplugins.ae.features.setsPreview.SetMenuClick;
import net.advancedplugins.ae.features.setsPreview.SetSpecificPreviewClick;
import net.advancedplugins.ae.features.setsPreview.SetsPreviewCommand;
import net.advancedplugins.ae.features.souls.SoulGem;
import net.advancedplugins.ae.features.souls.SoulGemClick;
import net.advancedplugins.ae.features.souls.SoulsEvents;
import net.advancedplugins.ae.features.tinkerer.ItemUse;
import net.advancedplugins.ae.features.tinkerer.TinkererInventory;
import net.advancedplugins.ae.features.usercommands.EnchantmentInfoCommand;
import net.advancedplugins.ae.features.usercommands.UserEnchantmentsCommand;
import net.advancedplugins.ae.features.usercommands.UserEnchantmentsHandler;
import net.advancedplugins.ae.features.weapons.WeaponHandler;
import net.advancedplugins.ae.globallisteners.InventoryClicks;
import net.advancedplugins.ae.globallisteners.ItemInteraction;
import net.advancedplugins.ae.globallisteners.listeners.AdminChatListener;
import net.advancedplugins.ae.globallisteners.listeners.AdminClickEvent;
import net.advancedplugins.ae.globallisteners.listeners.ClickListener;
import net.advancedplugins.ae.globallisteners.listeners.EnchantmentTableRightClick;
import net.advancedplugins.ae.globallisteners.listeners.GrindstoneListener;
import net.advancedplugins.ae.globallisteners.listeners.ItemFrameInteractEvent;
import net.advancedplugins.ae.globallisteners.listeners.LeaveEvent;
import net.advancedplugins.ae.globallisteners.listeners.ReloadEvent;
import net.advancedplugins.ae.globallisteners.listeners.UsedActionbarListener;
import net.advancedplugins.ae.globallisteners.listeners.WorldFilter;
import net.advancedplugins.ae.handlers.antiglitch.SimilarEnchantmentsHelper;
import net.advancedplugins.ae.handlers.antiglitch.SoulgemCraftEvent;
import net.advancedplugins.ae.handlers.anvil.AnvilEvent;
import net.advancedplugins.ae.handlers.commands.AEGiveCommand;
import net.advancedplugins.ae.handlers.commands.AlchemistCommand;
import net.advancedplugins.ae.handlers.commands.ApplyCommand;
import net.advancedplugins.ae.handlers.commands.EnchanterCommand;
import net.advancedplugins.ae.handlers.commands.GKitsCommand;
import net.advancedplugins.ae.handlers.commands.MainCommand;
import net.advancedplugins.ae.handlers.commands.TinkererCommand;
import net.advancedplugins.ae.handlers.commands.WithdrawSouls;
import net.advancedplugins.ae.handlers.lootPopulation.PopulationHandler;
import net.advancedplugins.ae.handlers.lootPopulation.listeners.VillagerLootPopulator;
import net.advancedplugins.ae.handlers.lootPopulation.listeners.WorldLootPopulator;
import net.advancedplugins.ae.handlers.netsharing.MarketInventory;
import net.advancedplugins.ae.handlers.placeholders.PapiPlaceholders;
import net.advancedplugins.ae.impl.effects.effects.EffectsHandler;
import net.advancedplugins.ae.impl.effects.handler.BowFullChangeHandler;
import net.advancedplugins.ae.impl.effects.handler.PlayerPlacedBlocksHandler;
import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.EntitySpawnUtils;
import net.advancedplugins.ae.impl.utils.ReallyFastBlockHandler;
import net.advancedplugins.ae.impl.utils.Registry;
import net.advancedplugins.ae.impl.utils.RemoveDeathItems;
import net.advancedplugins.ae.impl.utils.RunnableMetrics;
import net.advancedplugins.ae.impl.utils.hooks.HookPlugin;
import net.advancedplugins.ae.impl.utils.hooks.HooksHandler;
import net.advancedplugins.ae.impl.utils.nbt.utils.MinecraftVersion;
import net.advancedplugins.ae.impl.utils.plugin.FirstInstall;
import net.advancedplugins.ae.items.HolyWhiteScroll;
import net.advancedplugins.ae.items.ItemLoader;
import net.advancedplugins.ae.items.StatTrak;
import net.advancedplugins.ae.utils.AManager;
import net.advancedplugins.ae.utils.ConfigUpdater;
import net.advancedplugins.ae.utils.EnchantsConverter;
import net.advancedplugins.ae.utils.YamlFile;
import net.advancedplugins.ae.utils.YamlFolder;
import net.advancedplugins.ae.utils.lang.Lang;
import net.advancedplugins.ae.utils.lang.LangConverter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class Core extends JavaPlugin {
   private static Core instance;
   private static String pluginVersion;
   private static Economy economy = null;
   private static String whiteScroll = "";
   private static Set<Material> applicableItems;
   private static PaymentHandler paymentHandler;
   private static SetsManager setsManager;
   private static WeaponHandler weaponHandler;
   private static PopulationHandler populationHandler;
   private static SimilarEnchantmentsHelper similarEnchantmentsHelper;
   private static ItemLoader itemLoader;
   private static EffectsHandler effectsHandler;
   private UserEnchantmentsHandler userEnchantmentsHandler;
   private String lastId = "";
   private String serverUUID;
   private boolean again = false;
   private boolean lastTime = false;

   public void onEnable() {
      EntitySpawnUtils.metaDataPrefix = "ae";
      instance = this;
      ASManager.setInstance(this);
      FirstInstall.checkFirstInstall(this, "config.yml", "https://ae.advancedplugins.net/advancedenchantments-ui");
      pluginVersion = this.getDescription().getVersion();
      File plFolder = getInstance().getDataFolder();
      if (!plFolder.exists() || !plFolder.isDirectory()) {
         plFolder.mkdirs();
      }

      YamlFile.ANVIL.getFile();
      YamlFolder.ARMOR_SETS.getDataFiles();
      new RunnableMetrics(this, 2005);
      MinecraftVersion.init();
      if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_17_R1)) {
         this.getLogger().severe("AdvancedEnchantments only supports 1.17 or newer Minecraft versions.");
         this.getLogger().severe("Last plugin's version to support 1.8.8: 9.0.0b81, 1.17 or lower: 9.8.0 - download it from previous versions.");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         this.getLogger().info("Loading Minecraft Version " + MinecraftVersion.getVersion().name());
         HooksHandler.hook(this);
         if (!Lang.init(YamlFile.CONFIG.getString("language", "en-us"))) {
            LangConverter.convert();
         }

         AdvancedGroup.update();
         Values.init();
         GKitsCommand.init();
         setsManager = new SetsManager(getInstance());
         weaponHandler = new WeaponHandler(getInstance());
         AdvancedGroup.loadGroups();
         if (MinecraftVersion.getVersionNumber() >= 1160) {
            populationHandler = new PopulationHandler(getInstance());
         }

         Bukkit.getGlobalRegionScheduler().runDelayed(getInstance(), (t) -> this.onPostLoad(), 2L);
         paymentHandler = new PaymentHandler();
         List<String> configItems = YamlFile.CONFIG
            .getStringList(
               "items.settings.can-apply-to",
               Arrays.asList(
                  "ALL_SWORD", "ALL_ARMOR", "ALL_PICKAXE", "ALL_AXE", "ALL_SHOVEL", "ALL_HOE", "ALL_EDIBLE", "ALL_BLOCK", "BOOK", "ENCHANTED_BOOK", "BOW"
               )
            );
         applicableItems = AManager.initMaterials(configItems);
         new LevelsPayment(this);
         new DiamondsPayment(this);
         new EmeraldsPayment(this);
         new GoldIngotsPayment(this);
         new ConfigUpdater().resetTinkerer();
         Bukkit.getGlobalRegionScheduler().runDelayed(instance, (t) -> {
            TinkererInventory.init();
            new AlchemistInventory();
         }, 15L);
         if (!YamlFile.CONFIG.contains("version")) {
            this.getConfig().set("version", 1.0);
            this.getLogger().severe("Starting conversion of effects to Abilities V2 system...");
            EnchantsConverter.start(this);
            this.getLogger()
               .severe(
                  "Conversion completed. This is an assist for conversion, it will not update everything to new format. Please look over configuration and make sure everything is in the new format. Examples of what will not be updated:"
               );
            this.getLogger().severe("AOE effects, CUSTOM_TRENCH, removed effects and more...");
            this.saveConfig();
            this.reloadConfig();
         }

         effectsHandler = new EffectsHandler("ae", this, new ItemEnchantsReader());
         Bukkit.getGlobalRegionScheduler().runDelayed(this, (t) -> {
            AEnchantmentSorting.start();
            AEnchants.initializeEnchantsListBlacklist();
         }, 40L);
         itemLoader = new ItemLoader(this);
         new SoulGem();
         new WorldFilter(this);
         new Orb();
         GKits.setStatus(YamlFile.GKITS.getBoolean("gkits-enabled"));

         try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap)bukkitCommandMap.get(Bukkit.getServer());
            String command = "enchanter";
            if (this.isEnabled(command)) {
               commandMap.register(command, new EnchanterCommand(this.getCommandString(command)));
            }

            command = "tinkerer";
            if (this.isEnabled(command)) {
               commandMap.register(command, new TinkererCommand(this.getCommandString(command)));
            }

            command = "withdrawsouls";
            if (this.isEnabled(command)) {
               commandMap.register(command, new WithdrawSouls(this.getCommandString(command)));
            }

            command = "alchemist";
            if (this.isEnabled(command)) {
               commandMap.register(command, new AlchemistCommand(this.getCommandString(command)));
            }

            command = "apply";
            if (this.isEnabled(command)) {
               commandMap.register(command, new ApplyCommand(this.getCommandString(command)));
            }

            commandMap.register("aegive", new AEGiveCommand("aegive"));
            commandMap.register("advancedsets", new SetsCommand());
            command = GKitsCommand.getGkitCommand().get(0).replace("/", "");
            if (GKits.isEnabled()) {
               commandMap.register(command, new GKitsCommand(command));
            }

            if (this.isCustomCommandEnabled("enchantments")) {
               command = this.getCustomCommandString("enchantments", "/enchants");
               commandMap.register(command, new UserEnchantmentsCommand(command));
            }

            if (this.isCustomCommandEnabled("enchantment-info")) {
               command = this.getCustomCommandString("enchantment-info", "/enchant");
               commandMap.register(command, new EnchantmentInfoCommand(command));
            }
         } catch (Exception var7) {
            var7.printStackTrace();
         }

         this.getCommand("AdvancedEnchantments").setExecutor(new MainCommand());
         this.getCommand("AdvancedEnchantments").setTabCompleter(new MainCommand());
         MarketInventory.init(this);
         new ETableHandler();
         if (this.setupEconomy()) {
            instance.getLogger().info("Successfully hooked into economy plugin (Vault)");
         }

         Bukkit.getAsyncScheduler().runAtFixedRate(this, (t) -> {
            YamlFile.PDATA.save();
         }, 6000L * 50L, 6000L * 50L, java.util.concurrent.TimeUnit.MILLISECONDS);
         setWhiteScroll(YamlFile.CONFIG.getString("white-scroll.item.name", "&fWhite Scroll"));
         similarEnchantmentsHelper = new SimilarEnchantmentsHelper();

         try {
            this.lastId = Registry.get();
            Bukkit.getGlobalRegionScheduler().execute(this, () -> {
                  Core.this.again = true;
                  MainCommand.name = "pluginLoaded";
               });
         } catch (Exception var6) {
            var6.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(instance);
         }

         if (!Registry.get().equalsIgnoreCase(this.lastId)) {
            Bukkit.getPluginManager().disablePlugin(this);
         }

         this.registerListeners();
         ReallyFastBlockHandler.init();
         if (HooksHandler.getHook(HookPlugin.PLACEHOLDERAPI) != null) {
            new PapiPlaceholders().register();
         }
      }
   }

   private void onPostLoad() {
      if (MinecraftVersion.getVersionNumber() >= 1160) {
         if (getPopulationHandler().getRandomLootMap().isEnabled()) {
            for (World w : Bukkit.getWorlds()) {
               Stream.of(w.getPopulators()).filter(p -> p instanceof WorldLootPopulator).forEach(p -> w.getPopulators().remove(p));
               w.getPopulators().add(new WorldLootPopulator());
            }
         }

         if (getPopulationHandler().getVillagerLootMap().isEnabled()) {
            Bukkit.getPluginManager().registerEvents(new VillagerLootPopulator(), getInstance());
         }
      }
   }

   private void registerListeners() {
      PluginManager p = Bukkit.getPluginManager();
      p.registerEvents(new ReloadEvent(), instance);
      p.registerEvents(new AdminClickEvent(), instance);
      p.registerEvents(new AdminChatListener(), instance);
      p.registerEvents(new ClickListener(), instance);
      p.registerEvents(new LeaveEvent(), instance);
      p.registerEvents(new UsedActionbarListener(), instance);
      p.registerEvents(new InventoryEvent(), instance);
      p.registerEvents(new TinkererInventory(), instance);
      p.registerEvents(new GClick(), instance);
      p.registerEvents(new ConfirmInventory(), instance);
      p.registerEvents(new SetMenuClick(), instance);
      p.registerEvents(new SetsPreviewCommand(), instance);
      p.registerEvents(new SetSpecificPreviewClick(), instance);
      p.registerEvents(new ItemFrameInteractEvent(), instance);
      p.registerEvents(new ItemInteraction(), instance);
      p.registerEvents(new InventoryClicks(), instance);
      p.registerEvents(new StatTrak(), instance);
      p.registerEvents(new SoulsEvents(), instance);
      p.registerEvents(new AnvilEvent(), instance);
      p.registerEvents(new GListeners(), instance);
      p.registerEvents(new BookRightClick(), instance);
      p.registerEvents(new InteractEvent(), instance);
      p.registerEvents(new ItemUse(), instance);
      p.registerEvents(new SkullPlace(), instance);
      p.registerEvents(new SoulGemClick(), instance);
      p.registerEvents(new SoulgemCraftEvent(), instance);
      p.registerEvents(new MobDrops(), instance);
      p.registerEvents(new MarketInventory(), instance);
      p.registerEvents(new AlchemistInventoryClicks(), instance);
      p.registerEvents(new HolyWhiteScroll(), instance);
      p.registerEvents(new RemoveDeathItems(), instance);
      p.registerEvents(new EntitySpawnUtils(this), this);
      p.registerEvents(new SetEquipEvent(), instance);
      p.registerEvents(new SetEquipEvent(), instance);
      p.registerEvents(new OrbApply(), instance);
      p.registerEvents(new PlayerPlacedBlocksHandler(this), this);
      p.registerEvents(new BowFullChangeHandler(), this);
      if (YamlFile.CONFIG.getBoolean("settings.open-enchanter-by-right-clicking-enchantment-table", false)) {
         p.registerEvents(new EnchantmentTableRightClick(), this);
      }

      if (YamlFile.CONFIG.getBoolean("settings.grindstones-remove-custom-enchants", true) && MinecraftVersion.getVersionNumber() >= 1160) {
         p.registerEvents(new GrindstoneListener.SpigotListener(), instance);
         if (MinecraftVersion.getVersionNumber() >= 1180) {
            p.registerEvents(new GrindstoneListener.Grindstone1_18_Listener(), instance);
         }
      }
   }

   public static String getWhiteScrollName() {
      return getWhiteScroll();
   }

   private static String getWhiteScroll() {
      return whiteScroll;
   }

   public static void setWhiteScroll(String whiteScroll) {
      Core.whiteScroll = whiteScroll;
   }

   public void onDisable() {
      MarketInventory.unload();
      YamlFile.PDATA.save();
      Bukkit.getGlobalRegionScheduler().cancelTasks(this);
      effectsHandler.unload();
   }

   public ItemLoader getItemHandler() {
      return itemLoader;
   }

   private boolean isEnabled(String command) {
      return YamlFile.CONFIG.getBoolean("commands." + command + ".enabled", true);
   }

   private boolean isCustomCommandEnabled(String section) {
      return YamlFile.COMMANDS.getBoolean(section + ".enabled", false);
   }

   private String getCommandString(String command) {
      String rt = YamlFile.CONFIG.getString("commands." + command + ".command", command);
      return rt.replaceAll("/", "");
   }

   private String getCustomCommandString(String section, String command) {
      String rt = YamlFile.COMMANDS.getString(section + ".command", command);
      return rt.replaceAll("/", "");
   }

   private boolean setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            return false;
         } else {
            economy = (Economy)rsp.getProvider();
            return true;
         }
      }
   }

   public static boolean canApplyTo(Material m, ItemStack item) {
      return LocalAPI.isValidForEnchantments(item) ? true : applicableItems.contains(m);
   }

   public Core() {
   }

   protected Core(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
      super(loader, description, dataFolder, file);
   }

   public static Core getInstance() {
      return instance;
   }

   public static String getPluginVersion() {
      return pluginVersion;
   }

   public static Economy getEconomy() {
      return economy;
   }

   public static PaymentHandler getPaymentHandler() {
      return paymentHandler;
   }

   public static SetsManager getSetsManager() {
      return setsManager;
   }

   public static WeaponHandler getWeaponHandler() {
      return weaponHandler;
   }

   public static PopulationHandler getPopulationHandler() {
      return populationHandler;
   }

   public static SimilarEnchantmentsHelper getSimilarEnchantmentsHelper() {
      return similarEnchantmentsHelper;
   }

   public static ItemLoader getItemLoader() {
      return itemLoader;
   }

   public static EffectsHandler getEffectsHandler() {
      return effectsHandler;
   }
}
