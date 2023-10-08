package com.jtprince.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Set;

/**
 * Class exclusively used for debugging packets. Should not be used in released versions of the plugin.
 */
public class PacketDebugger extends PacketAdapter implements Listener {
    private final JavaPlugin plugin;

    private static final Set<PacketType> PACKETS_CLIENTBOUND = Set.of(
            PacketType.Play.Server.RESPAWN,
            PacketType.Play.Server.POSITION,
            PacketType.Play.Server.SPAWN_POSITION,
            PacketType.Play.Server.MAP_CHUNK,
            PacketType.Play.Server.UNLOAD_CHUNK,
            PacketType.Play.Server.INITIALIZE_BORDER,
            PacketType.Play.Server.SET_BORDER_CENTER
    );

    public PacketDebugger(JavaPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, PACKETS_CLIENTBOUND);
        this.plugin = plugin;
    }

    public void registerAdapters() {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

        @Override
    public void onPacketSending(PacketEvent event) {
        log("PACKET: " + event.getPacket().getType().name());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        log("EVENT: PlayerRespawnEvent");
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        log("EVENT: PlayerLoginEvent");
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        log("EVENT: PlayerJoinEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        log("EVENT: PlayerSpawnLocationEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        log("EVENT: PlayerTeleportEvent");
    }

    private void log(String message) {
        plugin.getLogger().fine(message);
    }
}
