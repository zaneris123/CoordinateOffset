package com.jtprince.coordinateoffset;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoordinateOffset extends JavaPlugin {
    static CoordinateOffset instance;
    private static PlayerOffsetsManager playerOffsetsManager;
    static OffsetProvider provider;


    @Override
    public void onEnable() {
        instance = this;
        playerOffsetsManager = new PlayerOffsetsManager();
//        provider = new ConstantOffsetProvider(new Offset(1024, 1024));
        provider = new RandomizedOffsetProvider();

        Bukkit.getPluginManager().registerEvents(new BukkitEventListener(), this);

        new PacketOffsetAdapter(this).registerAdapters();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static PlayerOffsetsManager getPlayerManager() {
        return playerOffsetsManager;
    }
}
