package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.RandomizedOffsetProvider;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CoordinateOffset extends JavaPlugin {
    private static CoordinateOffset instance;
    private static PlayerOffsetsManager playerOffsetsManager;
    private static OffsetProvider provider;


    @Override
    public void onEnable() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true).verboseOutput(false));
        CommandAPI.onEnable();

        instance = this;
        saveDefaultConfig();

        playerOffsetsManager = new PlayerOffsetsManager();
//        provider = new ConstantOffsetProvider(new Offset(1024, 1024));
        provider = new RandomizedOffsetProvider();

        Bukkit.getPluginManager().registerEvents(new BukkitEventListener(playerOffsetsManager), this);

        new PacketOffsetAdapter(this).registerAdapters();

        new CommandAPICommand("offset")
                .withPermission(CommandPermission.OP)
                .executesPlayer((player, args) -> {

                })
                .register();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static CoordinateOffset getInstance() {
        return instance;
    }

    public static PlayerOffsetsManager getPlayerManager() {
        return playerOffsetsManager;
    }

    static @NotNull Offset provideOffset(@NotNull Player player, @NotNull World world, @Nullable String logReason) {
        Offset offset = provider.getOffset(player, world);
        if (instance.getConfig().getBoolean("verbose")) {
            instance.getLogger().info("Created " + offset + " for " + player.getName() + " in " + world.getName() + (logReason == null ? "." : (" (" + logReason + ").")));
        }
        return offset;
    }
}
