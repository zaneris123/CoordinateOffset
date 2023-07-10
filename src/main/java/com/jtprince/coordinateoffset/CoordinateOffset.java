package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.provider.ConstantOffsetProvider;
import com.jtprince.coordinateoffset.provider.RandomOffsetProvider;
import com.jtprince.coordinateoffset.provider.RandomPersistentOffsetProvider;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoordinateOffset extends JavaPlugin {
    private static CoordinateOffset instance;
    private static PlayerOffsetsManager playerOffsetsManager;
    private static OffsetProviderManager providerManager;

    @Override
    public void onEnable() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true).verboseOutput(false));
        CommandAPI.onEnable();

        instance = this;
        saveDefaultConfig();

        playerOffsetsManager = new PlayerOffsetsManager();
        Bukkit.getPluginManager().registerEvents(new BukkitEventListener(playerOffsetsManager), this);

        providerManager = new OffsetProviderManager();
        providerManager.registerConfigurationFactory("ConstantOffsetProvider", new ConstantOffsetProvider.ConfigFactory());
        providerManager.registerConfigurationFactory("RandomOffsetProvider", new RandomOffsetProvider.ConfigFactory());
        providerManager.registerConfigurationFactory("RandomPersistentOffsetProvider", new RandomPersistentOffsetProvider.ConfigFactory());

        // TBD: Allow extensions to register their providers first.
        int providers = providerManager.loadProvidersFromConfig(getConfig());
        getLogger().info("Loaded " + providers + " offset providers from config.");

        new PacketOffsetAdapter(this).registerAdapters();

        registerCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reload() {
        reloadConfig();
        providerManager.loadProvidersFromConfig(getConfig());
        getLogger().info("Config reloaded.");
    }

    public static CoordinateOffset getInstance() {
        return instance;
    }

    public static PlayerOffsetsManager getPlayerManager() {
        return playerOffsetsManager;
    }

    public static OffsetProviderManager getOffsetProviderManager() {
        return providerManager;
    }

    private void registerCommands() {
        new CommandAPICommand("offset")
            .withPermission(CommandPermission.OP)
            .withArguments(LiteralArgument.of("reload"))
            .executes((sender, args) -> {
                try {
                    reload();
                    sender.sendMessage("Reloaded CoordinateOffset config. Players may need to relog to see the changes.");
                } catch (Exception e) {
                    sender.sendMessage("Failed to reload the config. Check the console for details.");
                    e.printStackTrace();
                }
            })
            .register();
    }
}
