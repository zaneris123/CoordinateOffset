package com.jtprince.coordinateoffset;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.warp.coordinatesobfuscator.TranslatorClientbound;
import org.warp.coordinatesobfuscator.TranslatorServerbound;

import java.util.logging.Logger;

class PacketOffsetAdapter {
    private final CoordinateOffset plugin;
    private final Logger logger;

    PacketOffsetAdapter(CoordinateOffset plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    void registerAdapters() {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new AdapterServer());
        pm.addPacketListener(new AdapterClient());
    }

    private class AdapterServer extends PacketAdapter {
        private AdapterServer() {
            super(PacketOffsetAdapter.this.plugin, ListenerPriority.HIGHEST, TranslatorClientbound.PACKETS_SERVER);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            var packet = event.getPacket();

            if (packet.getType() == Server.LOGIN) {
                PacketOffsetAdapter.this.plugin.impulseOffsetChange(new OffsetProviderContext(
                        event.getPlayer(), event.getPlayer().getWorld(),
                        event.getPlayer().getLocation(), OffsetProviderContext.ProvideReason.JOIN,
                        PacketOffsetAdapter.this.plugin));
            }

            Offset offset;
            if (packet.getType() == Server.RESPAWN) {
                /*
                 * The respawn packet has a unique need: it's the packet that causes a world change. However, the packet
                 * itself contains a coordinate for the world the player is going *to*. If we just use the regular
                 * Player#getWorld function, we'll get the offset for the world the player is coming *from*.
                 * We shouldn't validate that the offset lookup has a matching World in this case only.
                 */
                offset = PacketOffsetAdapter.this.plugin.getPlayerManager().get(event.getPlayer(), null);
            } else {
                offset = PacketOffsetAdapter.this.plugin.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            }

            if (PacketOffsetAdapter.this.plugin.getConfig().getBoolean("obfuscateWorldBorder") &&
                    !offset.equals(Offset.ZERO) &&
                    TranslatorClientbound.PACKETS_SERVER_BORDER.contains(packet.getType())) {
                event.setCancelled(true);
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
            super(PacketOffsetAdapter.this.plugin, ListenerPriority.LOWEST, TranslatorServerbound.PACKETS_CLIENT);
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            var packet = event.getPacket();
            var offset = PacketOffsetAdapter.this.plugin.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            TranslatorServerbound.incoming(logger, packet, offset);
        }
    }
}
