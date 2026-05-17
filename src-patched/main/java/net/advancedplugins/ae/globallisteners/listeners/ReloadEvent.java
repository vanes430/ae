package net.advancedplugins.ae.globallisteners.listeners;

import net.advancedplugins.ae.Core;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerLoadEvent.LoadType;

public class ReloadEvent implements Listener {
   @EventHandler
   public void reloadEvent(ServerLoadEvent e) {
      if (e.getType() == LoadType.RELOAD) {
         Bukkit.getGlobalRegionScheduler().runDelayed(Core.getInstance(), (task) -> Bukkit.getOnlinePlayers().forEach(p -> {}), 2L);
      }
   }
}
