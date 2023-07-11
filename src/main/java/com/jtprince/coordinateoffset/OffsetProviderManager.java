package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OffsetProviderManager {
    private final Map<String, OffsetProvider.ConfigurationFactory<?>> configFactories = new HashMap<>();
    private Map<String, OffsetProvider> providersFromConfig = new HashMap<>();

    private OffsetProvider defaultProvider;
    private Map<UUID, OffsetProvider> perPlayerYamlProviders = new HashMap<>();

    private @NotNull Set<String> getOptionNames() {
        return providersFromConfig.keySet();
    }

    void registerConfigurationFactory(@NotNull String className, @NotNull OffsetProvider.ConfigurationFactory<?> factory) {
        configFactories.put(className, factory);
    }

    int loadProvidersFromConfig(@NotNull FileConfiguration config) throws IllegalArgumentException {
        ConfigurationSection providerSection = config.getConfigurationSection("offsetProviders");
        if (providerSection == null) {
            throw new IllegalArgumentException("Missing offsetProviders section from config!");
        }

        Map<String, OffsetProvider> newProviders = new HashMap<>();
        for (String providerName : providerSection.getKeys(false)) {
            ConfigurationSection section = providerSection.getConfigurationSection(providerName);
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

            OffsetProvider provider = factory.createProvider(providerName, section);
            newProviders.put(providerName, provider);
        }

        providersFromConfig = newProviders;

        // Load default provider
        String defaultProviderName = config.getString("defaultOffsetProvider");
        if (defaultProviderName == null) {
            throw new IllegalArgumentException("Missing defaultOffsetProvider from config!");
        }

        defaultProvider = providersFromConfig.get(defaultProviderName);
        if (defaultProvider == null) {
            throw new IllegalArgumentException("Unknown defaultOffsetProvider " + defaultProviderName + ". Options are " + String.join(", ", getOptionNames()));
        }

        // Load per-player overrides
        Map<UUID, OffsetProvider> newPerPlayer = new HashMap<>();
        ConfigurationSection perPlayerSection = config.getConfigurationSection("perPlayerOffsetProvider");
        if (perPlayerSection != null) {
            for (String uuidStr : perPlayerSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr); // throws IllegalArgumentException
                String value = perPlayerSection.getString(uuidStr);
                OffsetProvider provider = providersFromConfig.get(value);
                if (provider == null) {
                    throw new IllegalArgumentException("Unknown provider " + value + " for UUID " + uuidStr + ". Options are " + String.join(", ", getOptionNames()));
                }
                newPerPlayer.put(uuid, provider);
            }
        }
        perPlayerYamlProviders = newPerPlayer;

        return newProviders.size();
    }

    @NotNull Offset provideOffset(@NotNull Player player, @NotNull World world, @NotNull OffsetProvider.ProvideReason reason) {
        OffsetProvider provider = defaultProvider;
        ProviderSource providerSource = ProviderSource.DEFAULT;

        if (perPlayerYamlProviders.containsKey(player.getUniqueId())) {
            provider = perPlayerYamlProviders.get(player.getUniqueId());
            providerSource = ProviderSource.CONFIG;
        } else {
            LuckPermsIntegration lp = CoordinateOffset.getLuckPermsIntegration();
            if (lp != null) {
                String lpMetaStr = lp.getProviderOverride(player, world);
                if (lpMetaStr != null) {
                    provider = providersFromConfig.get(lpMetaStr);
                    if (provider == null) {
                        CoordinateOffset.getInstance().getLogger().severe("Unknown provider for LuckPerms meta lookup on " + player.getName() + ": " + lpMetaStr + ". Options are " + String.join(", ", getOptionNames()));
                        provider = defaultProvider;
                    } else {
                        providerSource = ProviderSource.LUCK_PERMS_META;
                    }
                }
            }
        }

        Offset offset = provider.getOffset(player, world, reason);

        if (CoordinateOffset.getInstance().getConfig().getBoolean("verbose")) {
            String reasonStr = null;
            switch (reason) {
                case JOIN -> reasonStr = "player joined";
                case RESPAWN -> reasonStr = "player respawned";
                case WORLD_CHANGE -> reasonStr = "player changed worlds";
                case DISTANT_TELEPORT -> reasonStr = "player teleported";
            }

            String sourceStr = null;
            switch (providerSource) {
                case DEFAULT -> sourceStr = "default provider";
                case CONFIG -> sourceStr = "config.yml override";
                case LUCK_PERMS_META -> sourceStr = "LuckPerms meta override";
            }

            CoordinateOffset.getInstance().getLogger().info("Using " + offset + " from provider \"" + provider.name + "\" (" + sourceStr + ") for player " + player.getName() + " in " + world.getName() + " (" + reasonStr + ").");
        }
        return offset;
    }

    enum ProviderSource {
        DEFAULT, CONFIG, LUCK_PERMS_META
    }

    void quitPlayer(@NotNull Player player) {
        for (OffsetProvider provider : providersFromConfig.values()) {
            provider.onPlayerQuit(player);
        }
    }
}
