package com.jtprince.coordinateoffset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.warp.coordinatesobfuscator.TranslatorClientbound;
import org.warp.coordinatesobfuscator.TranslatorServerbound;

import java.util.Set;

public class PacketOffsetAdapter {
    private final CoordinateOffset plugin;
    private static final Set<PacketType> PACKETS_SERVER = Set.of(
        // Server -> Client (Sending)
        Server.BUNDLE,
        Server.BLOCK_ACTION,
        Server.BLOCK_BREAK_ANIMATION,
        Server.BLOCK_CHANGE,
        Server.MULTI_BLOCK_CHANGE,
        Server.MAP_CHUNK,
        Server.UNLOAD_CHUNK,
        Server.LIGHT_UPDATE,
        Server.EXPLOSION,
        Server.SPAWN_POSITION,

        Server.RESPAWN,
        Server.POSITION,

        Server.WORLD_PARTICLES,
        Server.WORLD_EVENT,

        Server.NAMED_SOUND_EFFECT,

        Server.NAMED_ENTITY_SPAWN,
        Server.SPAWN_ENTITY,
        Server.SPAWN_ENTITY_EXPERIENCE_ORB,
        Server.ENTITY_TELEPORT,

        Server.OPEN_SIGN_EDITOR,

        Server.ENTITY_METADATA,
        Server.VIEW_CENTRE,
        Server.WINDOW_ITEMS,
        Server.WINDOW_DATA,
        Server.SET_SLOT,

        Server.TILE_ENTITY_DATA
    );

    private static final Set<PacketType> PACKETS_CLIENT = Set.of(
        // Client -> Server (Receiving)
        Client.POSITION,
        Client.POSITION_LOOK,
        Client.BLOCK_DIG,
        Client.BLOCK_PLACE,
        Client.USE_ITEM,
        Client.USE_ENTITY,
        Client.VEHICLE_MOVE,
        Client.SET_COMMAND_BLOCK,
        Client.SET_JIGSAW,
        Client.STRUCT,
        Client.UPDATE_SIGN
    );

    public PacketOffsetAdapter(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

    public void registerAdapters() {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new AdapterServer());
        pm.addPacketListener(new AdapterClient());
    }

    private class AdapterServer extends PacketAdapter {
        private AdapterServer() {
            super(PacketOffsetAdapter.this.plugin, ListenerPriority.HIGHEST, PACKETS_SERVER);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            var packet = event.getPacket();
            var offset = CoordinateOffset.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            TranslatorClientbound.outgoing(CoordinateOffset.instance.getLogger(), packet, offset);
            event.setPacket(packet);
        }
    }

    private class AdapterClient extends PacketAdapter {
        private AdapterClient() {
            super(PacketOffsetAdapter.this.plugin, ListenerPriority.LOWEST, PACKETS_CLIENT);
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            var packet = event.getPacket();
            var offset = CoordinateOffset.getPlayerManager().get(event.getPlayer(), event.getPlayer().getWorld());
            TranslatorServerbound.incoming(CoordinateOffset.instance.getLogger(), packet, offset);
        }
    }
}
