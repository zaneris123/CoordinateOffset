package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Objects;

public class BukkitEventListener implements Listener {
    private static final double MIN_TELEPORT_DISTANCE_TO_RESET = 2050;  // From upstream TranslatorClientbound#respawn
    private final PlayerOffsetsManager players;

    public BukkitEventListener(PlayerOffsetsManager playerOffsetsManager) {
        this.players = playerOffsetsManager;
    }

    private void impulseChange(@NotNull Player player, @NotNull World world, @NotNull OffsetProvider.ProvideReason reason) {
        Offset offset = CoordinateOffset.getOffsetProviderManager().provideOffset(player, world, reason);
        players.put(player, world, offset);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        /* Despite being named "SpawnLocation", this event is called every time a Player joins. */
        impulseChange(event.getPlayer(), Objects.requireNonNull(event.getSpawnLocation().getWorld()), OffsetProvider.ProvideReason.JOIN);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        impulseChange(event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()), OffsetProvider.ProvideReason.RESPAWN);
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

        impulseChange(event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()), reason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer());
        CoordinateOffset.getOffsetProviderManager().quitPlayer(event.getPlayer());
    }
}
