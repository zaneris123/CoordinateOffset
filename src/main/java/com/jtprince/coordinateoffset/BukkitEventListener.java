package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Objects;

public class BukkitEventListener implements Listener {
    private static final double MIN_TELEPORT_DISTANCE_TO_RESET = 2050;  // From upstream TranslatorClientbound#respawn
    private final PlayerOffsetsManager players;

    public BukkitEventListener(PlayerOffsetsManager playerOffsetsManager) {
        this.players = playerOffsetsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        /* Despite being named "SpawnLocation", this event is called every time a Player joins. */
        Offset offset = CoordinateOffset.provideOffset(event.getPlayer(), Objects.requireNonNull(event.getSpawnLocation().getWorld()), "player joined");
        players.put(event.getPlayer(), event.getSpawnLocation().getWorld(), offset);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Offset offset = CoordinateOffset.provideOffset(event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()), "player respawned");
        players.put(event.getPlayer(), event.getRespawnLocation().getWorld(), offset);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        boolean reset = false;
        if (event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
            reset = true;
        } else if (event.getFrom().distanceSquared(event.getTo()) > MIN_TELEPORT_DISTANCE_TO_RESET * MIN_TELEPORT_DISTANCE_TO_RESET) {
            reset = true;
        }

        if (!reset) return;

        Offset offset = CoordinateOffset.provideOffset(event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()), "player teleported");
        players.put(event.getPlayer(), event.getTo().getWorld(), offset);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer());
    }
}
