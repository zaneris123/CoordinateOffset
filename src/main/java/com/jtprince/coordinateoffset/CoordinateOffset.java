package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.provider.ConstantOffsetProvider;
import com.jtprince.coordinateoffset.provider.RandomOffsetProvider;
import com.jtprince.coordinateoffset.provider.ZeroAtLocationOffsetProvider;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CoordinateOffset extends JavaPlugin {
    private static CoordinateOffset instance;
    private PlayerOffsetsManager playerOffsetsManager;
    private OffsetProviderManager providerManager;

    @Override
    public void onEnable() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true).verboseOutput(false));
        CommandAPI.onEnable();

        instance = this;
        saveDefaultConfig();

        playerOffsetsManager = new PlayerOffsetsManager(this);
        providerManager = new OffsetProviderManager(this);
        new CoordinateOffsetCommands(this).registerCommands();
        Bukkit.getPluginManager().registerEvents(new BukkitEventListener(this, playerOffsetsManager), this);

        providerManager.registerConfigurationFactory("ConstantOffsetProvider", new ConstantOffsetProvider.ConfigFactory());
        providerManager.registerConfigurationFactory("RandomOffsetProvider", new RandomOffsetProvider.ConfigFactory());
        providerManager.registerConfigurationFactory("ZeroAtLocationOffsetProvider", new ZeroAtLocationOffsetProvider.ConfigFactory());

        new PacketOffsetAdapter(this).registerAdapters();

        // Wait to load providers until the first tick in case other plugins register their own providers.
        Bukkit.getScheduler().runTask(this, () -> providerManager.loadProvidersFromConfig(getConfig()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Reload the CoordinateOffset configuration defined in <code>config.yml</code>.
     */
    public void reload() {
        reloadConfig();
        providerManager.loadProvidersFromConfig(getConfig());
        getLogger().info("Config reloaded.");
    }

    /**
     * Get the loaded instance of the CoordinateOffset plugin on the server.
     * @return The instance of the plugin, or <code>null</code> if the plugin is not loaded.
     */
    @SuppressWarnings("unused")
    public static @Nullable CoordinateOffset getInstance() {
        return instance;
    }

    public boolean isVerboseLoggingEnabled() {
        return getConfig().getBoolean("verbose");
    }

    /**
     * Get the currently active coordinate {@link Offset} for a player.
     *
     * <p>For example, a player might have a coordinate Offset of <code>(128, 128)</code>. This would mean that the
     * player's "F3" menu reports that they are standing at <code>(0, 0)</code> when they are really standing at
     * <code>(128, 128)</code>.</p>
     *
     * <p>This Offset is subject to change, for example if the Player changes worlds.</p>
     *
     * @param player A player currently logged in to the server.
     * @return The coordinate Offset this player sees, or <code>Offset.ZERO</code> if the player has no offset.
     */
    @SuppressWarnings("unused")
    public @NotNull Offset getOffset(Player player) {
        return playerOffsetsManager.get(player, player.getWorld());
    }

    PlayerOffsetsManager getPlayerManager() {
        return playerOffsetsManager;
    }

    OffsetProviderManager getOffsetProviderManager() {
        return providerManager;
    }

    void impulseOffsetChange(@NotNull OffsetProviderContext context) {
        Offset offset = providerManager.provideOffset(context);
        playerOffsetsManager.put(context.player(), context.world(), offset);
    }
}
