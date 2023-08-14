package com.jtprince.coordinateoffset;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.Objects;

class BukkitEventListener implements Listener {
    private final CoordinateOffset plugin;
    private final PlayerOffsetsManager players;
    private final WorldBorderObfuscator worldBorderObfuscator;

    BukkitEventListener(CoordinateOffset plugin, PlayerOffsetsManager playerOffsetsManager, WorldBorderObfuscator worldBorderObfuscator) {
        this.plugin = plugin;
        this.players = playerOffsetsManager;
        this.worldBorderObfuscator = worldBorderObfuscator;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        plugin.onAllPluginsEnabled();
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
        } else if (event.getFrom().distanceSquared(event.getTo()) > getMinimumTeleportDistanceSquared(event.getTo().getWorld())) {
            boolean isTeleportDefinitelyRelative = false;
            try {
                // Extra Paper-only check - ensure that we're not attempting to offset a relative teleportation packet.
                var flags = event.getRelativeTeleportationFlags();
                if (flags.contains(TeleportFlag.Relative.X) || flags.contains(TeleportFlag.Relative.Z)) {
                    isTeleportDefinitelyRelative = true;
                }
            } catch (NoSuchMethodError err) {
                // Spigot does not support relative teleport flags. This is a Paper-only API.
            }

            if (!isTeleportDefinitelyRelative) {
                reason = OffsetProviderContext.ProvideReason.DISTANT_TELEPORT;
            }
        }

        if (reason == null) return;

        plugin.impulseOffsetChange(new OffsetProviderContext(
                event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()),
                event.getTo(), reason, plugin));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer());
        plugin.getOffsetProviderManager().quitPlayer(event.getPlayer());
        worldBorderObfuscator.onPlayerQuit(event.getPlayer());
    }

    private int getMinimumTeleportDistanceSquared(World world) {
        int viewDistance = world.getViewDistance();

        /*
         * Problem: If the player's offset changes when they teleport a short distance, the server won't re-send the
         * chunks that the server thinks the player already has. That means that the player will just never get some
         * chunks in their "new" location.
         * Easy Solution: Only allow an offset change when the player teleports if there are no overlapping chunks in
         * view distance before and after the teleport.
         * Future Solution: Find a way to resend all visible chunks on demand. Paper's Player#setSendViewDistance or
         * World#refreshChunk might be promising.
         */
        int minimumBlocks = ((viewDistance + 1) * 2) * 16;

        if (plugin.getConfig().isInt("distantTeleportMinimumDistance")) {
            // TODO: Not documented for now. Need to either fix the problem described above or document around it.
            minimumBlocks = plugin.getConfig().getInt("distantTeleportMinimumDistance");
        }

        return minimumBlocks * minimumBlocks;
    }
}
