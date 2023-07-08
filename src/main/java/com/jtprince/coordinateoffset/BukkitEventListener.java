package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Objects;

public class BukkitEventListener implements Listener {
    private static final double MIN_TELEPORT_DISTANCE_TO_RESET = 2050;  // From upstream TranslatorClientbound#respawn

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        /* Despite being named "SpawnLocation", this event is called every time a Player joins. */
        Offset offset = CoordinateOffset.provider.getOffset(event.getPlayer(), Objects.requireNonNull(event.getSpawnLocation().getWorld()));
        CoordinateOffset.getPlayerManager().put(event.getPlayer(), event.getSpawnLocation().getWorld(), offset);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Offset offset = CoordinateOffset.provider.getOffset(event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()));
        CoordinateOffset.getPlayerManager().put(event.getPlayer(), event.getRespawnLocation().getWorld(), offset);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        boolean reset = false;
        if (event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
            reset = true;
        } else if (event.getFrom().distanceSquared(event.getTo()) > MIN_TELEPORT_DISTANCE_TO_RESET * MIN_TELEPORT_DISTANCE_TO_RESET) {
            reset = true;
        }

        if (!reset) return;

        Offset offset = CoordinateOffset.provider.getOffset(event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()));
        CoordinateOffset.getPlayerManager().put(event.getPlayer(), event.getTo().getWorld(), offset);
    }
}
