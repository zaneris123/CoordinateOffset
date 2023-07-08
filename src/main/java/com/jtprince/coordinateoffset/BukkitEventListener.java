package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class BukkitEventListener implements Listener {
    public BukkitEventListener() {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        /* Despite being named "SpawnLocation", this event is called every time a Player joins. */
        Offset offset = CoordinateOffset.provider.getOffset(event.getPlayer());
        CoordinateOffset.getPlayerManager().put(event.getPlayer(), event.getPlayer().getWorld(), offset);
    }
}
