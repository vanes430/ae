package net.advancedplugins.ae.impl.utils.plugin;

import net.advancedplugins.ae.impl.utils.ASManager;
import net.advancedplugins.ae.impl.utils.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateChecker {
   private static final String UPDATE_URL = "https://advancedplugins.net/api/v1/getVersion.php?plugin=";

   public static void checkUpdate(JavaPlugin plugin) {
      FoliaScheduler.runTaskLaterAsynchronously(
         plugin,
         () -> {
            String currentVersion = plugin.getDescription().getVersion();
            String pluginName = plugin.getDescription().getName();

            String latestVersion;
            try {
               latestVersion = ASManager.fetchJsonFromUrl("https://advancedplugins.net/api/v1/getVersion.php?plugin=" + pluginName);
            } catch (Exception var5) {
               var5.printStackTrace();
               return;
            }

            if (!latestVersion.isEmpty() && !isLatest(currentVersion, latestVersion)) {
               Bukkit.getConsoleSender()
                  .sendMessage(
                     ChatColor.translateAlternateColorCodes(
                        '&', "&7[" + pluginName + "] &eYou're using an outdated version of " + pluginName + ". A new version is available: &f" + latestVersion
                     )
                  );
               Bukkit.getConsoleSender()
                  .sendMessage(
                     ChatColor.translateAlternateColorCodes(
                        '&',
                        "&7["
                           + pluginName
                           + "] &7Keep your Advanced plugins up to date automatically with &bMintServers&7 Unlimited Hosting: &bhttps://mintservers.com/"
                     )
                  );
            }
         },
         20L
      );
   }

   private static boolean isLatest(String currentVersion, String latestVersion) {
      String[] currentVersionParts = currentVersion.split("\\.");
      String[] latestVersionParts = latestVersion.split("\\.");

      for (int i = 0; i < Math.min(currentVersionParts.length, latestVersionParts.length); i++) {
         int currentPart = Integer.parseInt(currentVersionParts[i]);
         int latestPart = Integer.parseInt(latestVersionParts[i]);
         if (currentPart < latestPart) {
            return false;
         }

         if (currentPart > latestPart) {
            return true;
         }
      }

      return currentVersionParts.length >= latestVersionParts.length;
   }
}
