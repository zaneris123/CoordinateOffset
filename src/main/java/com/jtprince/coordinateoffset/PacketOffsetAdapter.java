package com.jtprince.coordinateoffset;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.jtprince.coordinateoffset.offsetter.OffsetterRegistry;
import com.jtprince.util.PartialStacktraceLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

class PacketOffsetAdapter {
    private final CoordinateOffset coPlugin;
    private final Logger logger;
    private final PacketDebugger packetHistory;

    PacketOffsetAdapter(CoordinateOffset plugin) {
        this.coPlugin = plugin;
        this.logger = plugin.getLogger();
        this.packetHistory = new PacketDebugger(plugin);
    }

    void registerAdapters() {
        PacketEvents.getAPI().getEventManager().registerListener(new Listener());
        PacketEvents.getAPI().init();
    }

    private class Listener extends PacketListenerAbstract {
        Listener() {
            super(PacketListenerPriority.HIGH);
        }

        private static final Set<PacketType.Play.Server> PACKETS_WORLD_BORDER = Set.of(
                // These packets are translated in WorldBorderObfuscator, not this file.
                PacketType.Play.Server.INITIALIZE_WORLD_BORDER,
                PacketType.Play.Server.WORLD_BORDER_CENTER,
                PacketType.Play.Server.WORLD_BORDER_LERP_SIZE,
                PacketType.Play.Server.WORLD_BORDER_SIZE,
                PacketType.Play.Server.WORLD_BORDER_WARNING_DELAY,
                PacketType.Play.Server.WORLD_BORDER_WARNING_REACH
        );

        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (coPlugin.isDebugEnabled()) {
                packetHistory.logPacket(event.getUser(), event.getPacketType());
            }

            Runnable logic = () -> {
                try {
                    if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK
                            || event.getPacketType() == PacketType.Play.Server.UPDATE_VIEW_POSITION) {
                        Player player = event.getPlayer();
                        if (player != null) {
                            coPlugin.getPlayerManager().setPositionedWorld(player, player.getWorld());
                        }
                    }

                    Offset offset;
                    if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME
                            || event.getPacketType() == PacketType.Play.Server.RESPAWN) {
                        offset = coPlugin.getPlayerManager().getOffsetLookahead(event.getUser().getUUID());
                    } else {
                        Player player = event.getPlayer();
                        if (player == null) return;
                        try {
                            offset = coPlugin.getPlayerManager().getOffset(player);
                        } catch (Exception e) {
                            // Fallback: initialize offset cache if missing
                            OffsetProviderContext context = new OffsetProviderContext(
                                player,
                                player.getWorld(),
                                player.getLocation(),
                                OffsetProviderContext.ProvideReason.JOIN,
                                coPlugin
                            );
                            coPlugin.getPlayerManager().regenerateOffset(context);
                            coPlugin.getPlayerManager().setPositionedWorld(player, player.getWorld());
                            offset = coPlugin.getPlayerManager().getOffset(player);
                            logger.warning("[PacketOffsetAdapter] Offset cache was missing for player '" + player.getName() + "' during packet send (" + event.getPacketType().getName() + "). Initialized on the fly and positioned world set. This may indicate a race condition or Folia timing issue.");
                        }
                    }

                    if (offset == null) {
                        String playerName = null;
                        if (event.getPlayer() instanceof org.bukkit.entity.Player p) {
                            playerName = p.getName();
                        } else if (event.getUser() != null) {
                            playerName = event.getUser().getUUID().toString();
                        }
                        logger.warning("[PacketOffsetAdapter] Offset is still null for player '" + playerName + "' during packet send (" + event.getPacketType().getName() + "). Skipping packet offset.");
                        return;
                    }
                    if (offset.equals(Offset.ZERO)) return;

                    if (PACKETS_WORLD_BORDER.contains(event.getPacketType())) {
                        coPlugin.getWorldBorderObfuscator().translate(event, event.getPlayer());
                        return;
                    }

                    OffsetterRegistry.attemptToOffset(event, offset);
                } catch (Exception e) {
                    PartialStacktraceLogger.logStacktrace(logger, "Failed to apply offset for outgoing packet " +
                            event.getPacketType().getName() + " to " + event.getUser().getName(), e);
                    if (coPlugin.isDebugEnabled()) {
                        logger.warning("Packet history for above stacktrace: " + packetHistory.getHistory(event.getUser()));
                    }
                }
            };
            if (CoordinateOffset.isFoliaPresent && event.getPlayer() != null) {
                // Schedule on region thread using reflection
                try {
                    Object scheduler = event.getPlayer().getClass().getMethod("getScheduler").invoke(event.getPlayer());
                    scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class)
                            .invoke(scheduler, coPlugin, (java.util.function.Consumer<Object>) t -> logic.run(), null);
                } catch (Exception e) {
                    logger.severe("Failed to schedule packet send logic on Folia Player RegionScheduler: " + e.getMessage());
                }
            } else {
                logic.run();
            }
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (coPlugin.isDebugEnabled()) {
                packetHistory.logPacket(event.getUser(), event.getPacketType());
            }

            Runnable logic = () -> {
                try {
                    Player player = event.getPlayer();
                    if (player == null) return;

                    Offset offset;
                    try {
                        offset = coPlugin.getPlayerManager().getOffset(player, player.getWorld());
                    } catch (Exception e) {
                        // Fallback: initialize offset cache if missing
                        OffsetProviderContext context = new OffsetProviderContext(
                            player,
                            player.getWorld(),
                            player.getLocation(),
                            OffsetProviderContext.ProvideReason.JOIN,
                            coPlugin
                        );
                        coPlugin.getPlayerManager().regenerateOffset(context);
                        coPlugin.getPlayerManager().setPositionedWorld(player, player.getWorld());
                        offset = coPlugin.getPlayerManager().getOffset(player, player.getWorld());
                        logger.warning("[PacketOffsetAdapter] Offset cache was missing for player '" + player.getName() + "' during packet receive (" + event.getPacketType().getName() + "). Initialized on the fly and positioned world set. This may indicate a race condition or Folia timing issue.");
                    }
                    if (offset == null) {
                        String playerName = null;
                        if (event.getPlayer() instanceof org.bukkit.entity.Player p) {
                            playerName = p.getName();
                        } else if (event.getUser() != null) {
                            playerName = event.getUser().getUUID().toString();
                        }
                        logger.warning("[PacketOffsetAdapter] Offset is still null for player '" + playerName + "' during packet receive (" + event.getPacketType().getName() + "). Skipping packet un-offset.");
                        return;
                    }
                    if (offset.equals(Offset.ZERO)) return;

                    OffsetterRegistry.attemptToUnOffset(event, offset);
                } catch (Exception e) {
                    PartialStacktraceLogger.logStacktrace(logger, "Failed to reverse offset for incoming packet " +
                            event.getPacketType().getName() + " from " + event.getUser().getName(), e);
                    if (coPlugin.isDebugEnabled()) {
                        logger.warning("Packet history for above stacktrace: " + packetHistory.getHistory(event.getUser()));
                    }
                }
            };
            if (CoordinateOffset.isFoliaPresent && event.getPlayer() != null) {
                try {
                    Object scheduler = event.getPlayer().getClass().getMethod("getScheduler").invoke(event.getPlayer());
                    scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class)
                            .invoke(scheduler, coPlugin, (java.util.function.Consumer<Object>) t -> logic.run(), null);
                } catch (Exception e) {
                    logger.severe("Failed to schedule packet receive logic on Folia Player RegionScheduler: " + e.getMessage());
                }
            } else {
                logic.run();
            }
        }

        @Override
        public void onUserDisconnect(UserDisconnectEvent event) {
            UUID playerUuid = event.getUser().getUUID();
            if (playerUuid == null) return;

            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null
                    && PacketEvents.getAPI().getPlayerManager().getUser(onlinePlayer) != event.getUser()) {
                /*
                 * Special handling for attempting to "reconnect from another location":
                 * When a player reconnects from a second instance or location, the original TCP connection is closed.
                 * This eventually fires UserDisconnectEvent on the Netty thread.
                 *
                 * It is possible in certain previously-observed circumstances for UserDisconnectEvent to fire AFTER
                 * the new connection's Player is fully in-game. The result is that the Player's data is erased from
                 * the cache despite the second connection still being in-game, so we get "Unknown player for Offset
                 * lookup" ad infinitum.
                 *
                 * Solution: If the User (connection) object PacketEvents knows about for this Player is not the same
                 * connection that is closing, then the player must still be online in a new User. Therefore, do not
                 * wipe their data.
                 */
                return;
            }

            coPlugin.getPlayerManager().remove(playerUuid);
            coPlugin.getOffsetProviderManager().disconnectPlayer(playerUuid);
            coPlugin.getWorldBorderObfuscator().onPlayerDisconnect(playerUuid);

            if (coPlugin.isDebugEnabled()) {
                packetHistory.forget(event.getUser());
            }
        }
    }
}
