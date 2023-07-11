package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;

public class BukkitEventListener implements Listener {
    private static final double MIN_TELEPORT_DISTANCE_TO_RESET = 2050;  // From upstream TranslatorClientbound#respawn
    private final PlayerOffsetsManager players;

    public BukkitEventListener(PlayerOffsetsManager playerOffsetsManager) {
        this.players = playerOffsetsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        CoordinateOffset.impulseOffsetChange(event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()), OffsetProvider.ProvideReason.RESPAWN);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        OffsetProvider.ProvideReason reason = null;
        if (event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
            reason = OffsetProvider.ProvideReason.WORLD_CHANGE;
        } else if (event.getFrom().distanceSquared(event.getTo()) > MIN_TELEPORT_DISTANCE_TO_RESET * MIN_TELEPORT_DISTANCE_TO_RESET) {
            reason = OffsetProvider.ProvideReason.DISTANT_TELEPORT;
        }

        if (reason == null) return;

        CoordinateOffset.impulseOffsetChange(event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()), reason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer());
        CoordinateOffset.getOffsetProviderManager().quitPlayer(event.getPlayer());
    }
}
