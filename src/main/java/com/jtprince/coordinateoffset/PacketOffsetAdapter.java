package com.jtprince.coordinateoffset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.google.common.collect.Sets;
import com.jtprince.coordinateoffset.translator.Translator;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

class PacketOffsetAdapter {
    private final CoordinateOffset coPlugin;
    private final Logger logger;

    PacketOffsetAdapter(CoordinateOffset plugin) {
        this.coPlugin = plugin;
        this.logger = plugin.getLogger();
    }

    void registerAdapters() {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        Translator.Version translators = getTranslatorForRunningVersion();

        try {
            pm.addPacketListener(new AdapterServer(translators.clientbound().getDeclaredConstructor().newInstance()));
            pm.addPacketListener(new AdapterClient(translators.serverbound().getDeclaredConstructor().newInstance()));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create the packet listener! Please report this as a bug!", e);
        }
    }

    private Translator.Version getTranslatorForRunningVersion() {
        if (MinecraftVersion.getCurrentVersion().compareTo(Translator.LATEST_SUPPORTED) > 0) {
            String runningVersion = MinecraftVersion.getCurrentVersion().getVersion();
            logger.warning("This plugin version has not been tested with the protocol version in your server (" + runningVersion + ") yet.");
            logger.warning("Some packets may not be properly obfuscated, and others may create errors.");
            logger.warning("Please wait for an update or proceed at your own risk.");
        }

        Translator.Version chosen = null;
        boolean outdatedServer = true;
        for (Translator.Version v : Translator.VERSIONS) {
            chosen = v;
            if (v.minVersion().atOrAbove()) {
                outdatedServer = false;
                break;
            }
        }

        if (outdatedServer) {
            String earliest = Objects.requireNonNull(chosen).minVersion().getVersion();
            logger.severe("This plugin only supports Minecraft " + earliest + " and above - it will very likely break!");
        }

        StringBuilder s = new StringBuilder("Using protocol version for Minecraft ");
        s.append(chosen.minVersion().getVersion());
        if (chosen.maxStatedVersion() != null) {
            s.append(" through ").append(chosen.maxStatedVersion());
        }
        s.append('.');
        logger.info(s.toString());

        return chosen;
    }

    private class AdapterServer extends PacketAdapter {
        private final Translator.Clientbound translator;
        private AdapterServer(Translator.Clientbound translator) {
            super(coPlugin, ListenerPriority.HIGHEST, Sets.union(translator.getPacketTypes(), PACKETS_WORLD_BORDER));
            this.translator = translator;
        }

        private static final Set<PacketType> PACKETS_WORLD_BORDER = Set.of(
                // These packets are translated in WorldBorderObfuscator, not this file.
                PacketType.Play.Server.INITIALIZE_BORDER,
                PacketType.Play.Server.SET_BORDER_CENTER,
                PacketType.Play.Server.SET_BORDER_LERP_SIZE,
                PacketType.Play.Server.SET_BORDER_SIZE,
                PacketType.Play.Server.SET_BORDER_WARNING_DELAY,
                PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE
        );

        public void onPacketSending(PacketEvent event) {
            PacketContainer packet = event.getPacket();
            Player player = event.getPlayer();

            if (packet.getType() == PacketType.Play.Server.LOGIN) {
                OffsetProviderContext context = new OffsetProviderContext(
                        player, player.getWorld(), player.getLocation(),
                        OffsetProviderContext.ProvideReason.JOIN, coPlugin
                );
                coPlugin.getPlayerManager().regenerateOffset(context);
                coPlugin.getPlayerManager().setPositionedWorld(context.player(), context.world());
            }

            if (packet.getType() == PacketType.Play.Server.POSITION) {
                coPlugin.getPlayerManager().setPositionedWorld(player, player.getWorld());
            }

            Offset offset;
            if (packet.getType() == PacketType.Play.Server.RESPAWN) {
                /*
                 * Respawn packets need to apply a new world's offsets ahead of actually moving the player to that new
                 * world.
                 * See `docs/OffsetChangeHandling.md`
                 */
                offset = coPlugin.getPlayerManager().getOffsetLookahead(event.getPlayer());
            } else {
                offset = coPlugin.getPlayerManager().getOffset(event.getPlayer());
            }

            if (offset.equals(Offset.ZERO)) return;

            if (PACKETS_WORLD_BORDER.contains(packet.getType())) {
                // Border packets need special handling, more than just applying an offset with TranslatorClientbound
                coPlugin.getWorldBorderObfuscator().translate(packet, event.getPlayer());
                return;
            }

            packet = translator.translate(event, offset);
            if (packet != null) {
                event.setPacket(packet);
            } else {
                event.setCancelled(true);
            }
        }
    }

    private class AdapterClient extends PacketAdapter {
        private final Translator.Serverbound translator;
        private AdapterClient(Translator.Serverbound translator) {
            super(coPlugin, ListenerPriority.LOWEST, translator.getPacketTypes());
            this.translator = translator;
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            var offset = coPlugin.getPlayerManager().getOffset(event.getPlayer(), event.getPlayer().getWorld());
            if (offset.equals(Offset.ZERO)) return;

            PacketContainer packet = translator.translate(event, offset);
            if (packet != null) {
                event.setPacket(packet);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
