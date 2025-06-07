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
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

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
        if (CoordinateOffset.isFoliaPresent) {
            // Folia: Use GlobalRegionScheduler for server-wide events via reflection
            try {
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                Object globalScheduler = bukkitClass.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class)
                        .invoke(globalScheduler, plugin, (Runnable) plugin::onAllPluginsEnabled);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to schedule onAllPluginsEnabled with Folia GlobalRegionScheduler: " + e.getMessage());
            }
        } else {
            plugin.onAllPluginsEnabled();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Runnable logic = () -> {
            plugin.getPlayerManager().setPositionedWorld(event.getPlayer(), event.getSpawnLocation().getWorld());
            OffsetProviderContext context = new OffsetProviderContext(
                    event.getPlayer(), event.getSpawnLocation().getWorld(), event.getSpawnLocation(),
                    OffsetProviderContext.ProvideReason.JOIN, plugin
            );
            plugin.getPlayerManager().regenerateOffset(context);
        };
        if (CoordinateOffset.isFoliaPresent) {
            runOnFoliaPlayerScheduler(event.getPlayer(), plugin, logic);
        } else {
            logic.run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Runnable logic = () -> {
            OffsetProviderContext.ProvideReason reason = OffsetProviderContext.ProvideReason.DEATH_RESPAWN;
            try {
                if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) {
                    reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
                }
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                try {
                    if (event.getRespawnFlags().contains(PlayerRespawnEvent.RespawnFlag.END_PORTAL)) {
                        reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
                    }
                } catch (NoClassDefFoundError | NoSuchMethodError e2) {
                    plugin.getLogger().fine("No supported method for determining respawn reason.");
                }
            }

            var context = new OffsetProviderContext(
                    event.getPlayer(), Objects.requireNonNull(event.getRespawnLocation().getWorld()),
                    event.getRespawnLocation(), reason, plugin);
            plugin.getPlayerManager().regenerateOffset(context);
        };
        if (CoordinateOffset.isFoliaPresent) {
            runOnFoliaPlayerScheduler(event.getPlayer(), plugin, logic);
        } else {
            logic.run();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Runnable logic = () -> {
            OffsetProviderContext.ProvideReason reason = null;
            if (event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
                reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
            } else if (event.getFrom().distanceSquared(event.getTo()) > getMinimumTeleportDistanceSquared(event.getTo().getWorld())) {
                if (plugin.isUnsafeResetOnTeleportEnabled()) {
                    boolean isTeleportDefinitelyRelative = false;
                    try {
                        var flags = event.getRelativeTeleportationFlags();
                        if (flags.contains(TeleportFlag.Relative.X) || flags.contains(TeleportFlag.Relative.Z)) {
                            isTeleportDefinitelyRelative = true;
                        }
                    } catch (NoClassDefFoundError | NoSuchMethodError err) {
                        // Spigot does not support relative teleport flags. This is a Paper-only API.
                    }
                    if (!isTeleportDefinitelyRelative) {
                        reason = OffsetProviderContext.ProvideReason.DISTANT_TELEPORT;
                    }
                }
            }
            if (reason == null) return;
            var context = new OffsetProviderContext(
                    event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()),
                    event.getTo(), reason, plugin);
            plugin.getPlayerManager().regenerateOffset(context);
            worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getTo());
        };
        if (CoordinateOffset.isFoliaPresent) {
            runOnFoliaPlayerScheduler(event.getPlayer(), plugin, logic);
        } else {
            logic.run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Runnable logic = () -> worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getTo());
        if (CoordinateOffset.isFoliaPresent) {
            runOnFoliaPlayerScheduler(event.getPlayer(), plugin, logic);
        } else {
            logic.run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Runnable logic = () -> plugin.getOffsetProviderManager().quitPlayer(event.getPlayer());
        if (CoordinateOffset.isFoliaPresent) {
            runOnFoliaPlayerScheduler(event.getPlayer(), plugin, logic);
        } else {
            logic.run();
        }
    }

    /**
     * Schedules a task on the Folia Player RegionScheduler using reflection, if available.
     */
    private static void runOnFoliaPlayerScheduler(org.bukkit.entity.Player player, org.bukkit.plugin.Plugin plugin, Runnable logic) {
        try {
            Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
            scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) t -> logic.run(), null);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule task on Folia Player RegionScheduler: " + e.getMessage());
        }
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
