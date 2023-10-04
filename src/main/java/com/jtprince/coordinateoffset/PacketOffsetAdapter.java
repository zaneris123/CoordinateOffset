package com.jtprince.coordinateoffset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.Pair;
import com.google.common.collect.Sets;
import com.jtprince.coordinateoffset.translator.R1_19_4.TranslatorClientboundR1_19_4;
import com.jtprince.coordinateoffset.translator.R1_19_4.TranslatorServerboundR1_19_4;
import com.jtprince.coordinateoffset.translator.R1_20_2.TranslatorClientboundR1_20_2;
import com.jtprince.coordinateoffset.translator.R1_20_2.TranslatorServerboundR1_20_2;
import com.jtprince.coordinateoffset.translator.Translator;

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

        Pair<Translator, Translator> translators = getTranslatorForRunningVersion();

        pm.addPacketListener(new AdapterServer(translators.getFirst()));
        pm.addPacketListener(new AdapterClient(translators.getSecond()));
    }

    private Pair<Translator, Translator> getTranslatorForRunningVersion() {
        MinecraftVersion latestSupported = MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE;
        if (MinecraftVersion.getCurrentVersion().compareTo(latestSupported) > 0) {
            logger.warning("This plugin version has not been tested for your server version (" +
                    MinecraftVersion.getCurrentVersion().getVersion() + ") yet. Please wait for an update or" +
                    " proceed at your own risk.");
        }

        if (MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE.atOrAbove()) {
            logger.info("Using protocol version for Minecraft 1.20.2.");
            return new Pair<>(new TranslatorClientboundR1_20_2(), new TranslatorServerboundR1_20_2());
        }

        // Fall back on the earliest translator version
        if (!MinecraftVersion.FEATURE_PREVIEW_2.atOrAbove()) {
            logger.severe("This plugin only supports Minecraft 1.19.4 and above - it will very likely break!");
        }

        logger.info("Using protocol version for Minecraft 1.19.4 through 1.20.1.");
        return new Pair<>(new TranslatorClientboundR1_19_4(), new TranslatorServerboundR1_19_4());
    }

    private class AdapterServer extends PacketAdapter {
        private final Translator translator;
        private AdapterServer(Translator translator) {
            super(coPlugin, ListenerPriority.HIGHEST, Sets.union(translator.getPacketTypes(), PACKETS_SERVER_BORDER));
            this.translator = translator;
        }

        private static final Set<PacketType> PACKETS_SERVER_BORDER = Set.of(
                // These packets are translated in WorldBorderObfuscator, not this file.
                PacketType.Play.Server.INITIALIZE_BORDER,
                PacketType.Play.Server.SET_BORDER_CENTER,
                PacketType.Play.Server.SET_BORDER_LERP_SIZE,
                PacketType.Play.Server.SET_BORDER_SIZE,
                PacketType.Play.Server.SET_BORDER_WARNING_DELAY,
                PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE
        );

        public void onPacketSending(PacketEvent event) {
            var packet = event.getPacket();

            if (packet.getType() == PacketType.Play.Server.LOGIN) {
                coPlugin.impulseOffsetChange(new OffsetProviderContext(
                        event.getPlayer(), event.getPlayer().getWorld(),
                        event.getPlayer().getLocation(), OffsetProviderContext.ProvideReason.JOIN,
                        coPlugin));
            }

            Offset offset;
            if (packet.getType() == PacketType.Play.Server.RESPAWN || PACKETS_SERVER_BORDER.contains(packet.getType())) {
                /*
                 * The respawn packet has a unique need: it's the packet that causes a world change. However, the packet
                 * itself contains a coordinate for the world the player is going *to*. If we just use the regular
                 * Player#getWorld function, we'll get the offset for the world the player is coming *from*.
                 * We shouldn't validate that the offset lookup has a matching World in this case.
                 *
                 * Likewise, the server sends World Border packets for a new world *before* sending the respawn packet.
                 * The sequence goes Teleport Event (and offset change) -> Border packets -> Respawn packet.
                 */
                offset = coPlugin.getPlayerManager().get(event.getPlayer(), null);
            } else {
                offset = coPlugin.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            }

            if (offset.equals(Offset.ZERO)) return;

            if (PACKETS_SERVER_BORDER.contains(packet.getType())) {
                // Border packets need special handling, more than just applying an offset with TranslatorClientbound
                coPlugin.getWorldBorderObfuscator().translate(packet, event.getPlayer());
                return;
            }

            event.setPacket(translator.translate(packet, offset));
        }
    }

    private class AdapterClient extends PacketAdapter {
        private final Translator translator;
        private AdapterClient(Translator translator) {
            super(coPlugin, ListenerPriority.LOWEST, translator.getPacketTypes());
            this.translator = translator;
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            var packet = event.getPacket();
            var offset = coPlugin.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            if (offset.equals(Offset.ZERO)) return;
            event.setPacket(translator.translate(packet, offset));
        }
    }
}
