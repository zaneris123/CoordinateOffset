package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;

class BukkitEventListener implements Listener {
    private static final double MIN_TELEPORT_DISTANCE_TO_RESET = 2050;  // From upstream TranslatorClientbound#respawn

    private final CoordinateOffset plugin;
    private final PlayerOffsetsManager players;

    BukkitEventListener(CoordinateOffset plugin, PlayerOffsetsManager playerOffsetsManager) {
        this.plugin = plugin;
        this.players = playerOffsetsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        /*
         * The Respawn event is fired after using an End exit portal, but users probably expect that portal to trigger
         * a world change, not a death-based respawn.
         */
        OffsetProviderContext.ProvideReason reason;
        if (event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.DEATH) {
            reason = OffsetProviderContext.ProvideReason.DEATH_RESPAWN;
        } else {
            reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
        }
        plugin.impulseOffsetChange(new OffsetProviderContext(
                event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()),
                event.getRespawnLocation(), reason, plugin));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        OffsetProviderContext.ProvideReason reason = null;
        if (event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
            reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
        } else if (event.getFrom().distanceSquared(event.getTo()) > MIN_TELEPORT_DISTANCE_TO_RESET * MIN_TELEPORT_DISTANCE_TO_RESET) {
            reason = OffsetProviderContext.ProvideReason.DISTANT_TELEPORT;
        }

        if (reason == null) return;

        plugin.impulseOffsetChange(new OffsetProviderContext(
                event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()),
                event.getTo(), reason, plugin));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer());
        plugin.getOffsetProviderManager().quitPlayer(event.getPlayer());
    }
}
