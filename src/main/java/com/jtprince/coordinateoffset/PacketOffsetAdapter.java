package com.jtprince.coordinateoffset;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.warp.coordinatesobfuscator.TranslatorClientbound;
import org.warp.coordinatesobfuscator.TranslatorServerbound;

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
        pm.addPacketListener(new AdapterServer());
        pm.addPacketListener(new AdapterClient());

        if (!pm.getMinecraftVersion().isAtLeast(MinecraftVersion.FEATURE_PREVIEW_2)) {
            logger.severe("This plugin is only tested with Minecraft 1.19.4 and above - proceed at your own risk!");
        }
    }

    private class AdapterServer extends PacketAdapter {
        private AdapterServer() {
            super(coPlugin, ListenerPriority.HIGHEST, TranslatorClientbound.PACKETS_SERVER);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            var packet = event.getPacket();

            if (packet.getType() == Server.LOGIN) {
                coPlugin.impulseOffsetChange(new OffsetProviderContext(
                        event.getPlayer(), event.getPlayer().getWorld(),
                        event.getPlayer().getLocation(), OffsetProviderContext.ProvideReason.JOIN,
                        coPlugin));
            }

            Offset offset;
            if (packet.getType() == Server.RESPAWN ||
                    TranslatorClientbound.PACKETS_SERVER_BORDER.contains(packet.getType())) {
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

            if (TranslatorClientbound.PACKETS_SERVER_BORDER.contains(packet.getType())) {
                // Border packets need special handling, more than just applying an offset with TranslatorClientbound
                coPlugin.getWorldBorderObfuscator().translate(packet, event.getPlayer());
                return;
            }

            PacketContainer cloned = TranslatorClientbound.outgoing(logger, packet, offset);
            //noinspection ReplaceNullCheck
            if (cloned != null) {
                event.setPacket(cloned);
            } else {
                event.setPacket(packet);
            }
        }
    }

    private class AdapterClient extends PacketAdapter {
        private AdapterClient() {
            super(coPlugin, ListenerPriority.LOWEST, TranslatorServerbound.PACKETS_CLIENT);
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            var packet = event.getPacket();
            var offset = coPlugin.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            TranslatorServerbound.incoming(logger, packet, offset);
        }
    }
}
