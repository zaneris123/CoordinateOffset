package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OffsetProviderManager {
    private Map<String, OffsetProvider.ConfigurationFactory<?>> configFactories = new HashMap<>();
    private Map<String, OffsetProvider> providersFromConfig = new HashMap<>();

    private OffsetProvider defaultProvider;

    private @NotNull Set<String> getOptionNames() {
        return providersFromConfig.keySet();
    }

    void registerConfigurationFactory(@NotNull String className, @NotNull OffsetProvider.ConfigurationFactory<?> factory) {
        configFactories.put(className, factory);
    }

    int loadProvidersFromConfig(@NotNull FileConfiguration config) throws IllegalArgumentException {
        ConfigurationSection rootSection = config.getConfigurationSection("offsetProviders");
        if (rootSection == null) {
            throw new IllegalArgumentException("Missing offsetProviders section from config!");
        }

        Map<String, OffsetProvider> newProviders = new HashMap<>();
        for (String providerName : rootSection.getKeys(false)) {
            ConfigurationSection section = rootSection.getConfigurationSection(providerName);
            if (section == null) {
                throw new IllegalArgumentException("Offset provider " + providerName + " is not a valid configuration section.");
            }
            String className = section.getString("class");
            if (className == null) {
                throw new IllegalArgumentException("Offset provider " + providerName + " is missing a \"class\" field.");
            }
            OffsetProvider.ConfigurationFactory<?> factory = configFactories.get(className);
            if (factory == null) {
                throw new IllegalArgumentException("Offset provider " + providerName + " has an unknown class name " + className + ".");
            }

            OffsetProvider provider = factory.createProvider(section);
            newProviders.put(providerName, provider);
        }

        providersFromConfig = newProviders;

        String defaultProviderName = config.getString("defaultOffsetProvider");
        if (defaultProviderName == null) {
            throw new IllegalArgumentException("Missing defaultOffsetProvider from config!");
        }

        defaultProvider = providersFromConfig.get(defaultProviderName);
        if (defaultProvider == null) {
            throw new IllegalArgumentException("Unknown defaultOffsetProvider " + defaultProviderName + ". Options are " + String.join(", ", getOptionNames()));
        }

        return newProviders.size();
    }

    @NotNull Offset provideOffset(@NotNull Player player, @NotNull World world, @NotNull OffsetProvider.ProvideReason reason) {
        Offset offset = defaultProvider.getOffset(player, world, reason);
        if (CoordinateOffset.getInstance().getConfig().getBoolean("verbose")) {
            String reasonStr = null;
            switch (reason) {
                case JOIN -> reasonStr = "player joined";
                case RESPAWN -> reasonStr = "player respawned";
                case WORLD_CHANGE -> reasonStr = "player changed worlds";
                case DISTANT_TELEPORT -> reasonStr = "player teleported";
            }

            CoordinateOffset.getInstance().getLogger().info("Created " + offset + " for " + player.getName() + " in " + world.getName() + (reasonStr == null ? "." : (" (" + reasonStr + ").")));
        }
        return offset;
    }

    void quitPlayer(@NotNull Player player) {
        for (OffsetProvider provider : providersFromConfig.values()) {
            provider.onPlayerQuit(player);
        }
    }
}
