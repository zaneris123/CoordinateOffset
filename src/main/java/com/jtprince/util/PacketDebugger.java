package com.jtprince.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Class exclusively used for debugging packets. Should not be used in released versions of the plugin.
 */
public class PacketDebugger extends PacketAdapter {
    private final JavaPlugin plugin;

    private static final Set<PacketType> PACKETS_CLIENTBOUND = Set.of(
            PacketType.Play.Server.RESPAWN,
            PacketType.Play.Server.POSITION,
            PacketType.Play.Server.SPAWN_POSITION,
            PacketType.Play.Server.MAP_CHUNK,
            PacketType.Play.Server.UNLOAD_CHUNK
    );

    public PacketDebugger(JavaPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, PACKETS_CLIENTBOUND);
        this.plugin = plugin;
    }

    public void registerAdapters() {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(this);
    }

        @Override
    public void onPacketSending(PacketEvent event) {
        plugin.getLogger().info("PACKET: " + event.getPacket().getType().name());
        if (event.getPacket().getType().equals(PacketType.Play.Server.RESPAWN)) {
            World world = event.getPacket().getStructures().read(0).getWorldKeys().read(0);
            plugin.getLogger().info("RESPAWN: " + world.getName() + " " + world.getUID());
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {}
}
