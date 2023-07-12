package com.jtprince.coordinateoffset;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class OffsetProviderManager {
    private final CoordinateOffset plugin;
    private final Map<String, OffsetProvider.ConfigurationFactory<?>> configFactories = new HashMap<>();
    private Map<String, OffsetProvider> providersFromConfig = new HashMap<>();

    private OffsetProvider defaultProvider;
    private Map<UUID, OffsetProvider> perPlayerYamlProviders = new HashMap<>();

    OffsetProviderManager(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

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

            OffsetProvider provider = factory.createProvider(providerName, plugin, section);
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

    @NotNull Offset provideOffset(@NotNull OffsetProviderContext context) {
        OffsetProvider provider = defaultProvider;
        ProviderSource providerSource = ProviderSource.DEFAULT;

        if (perPlayerYamlProviders.containsKey(context.player().getUniqueId())) {
            provider = perPlayerYamlProviders.get(context.player().getUniqueId());
            providerSource = ProviderSource.CONFIG;
        } else {
            LuckPermsIntegration lp = plugin.getLuckPermsIntegration();
            if (lp != null) {
                String lpMetaStr = lp.getProviderOverride(context.player(), context.world());
                if (lpMetaStr != null) {
                    provider = providersFromConfig.get(lpMetaStr);
                    if (provider == null) {
                        plugin.getLogger().severe("Unknown provider for LuckPerms meta lookup on " + context.player().getName() + ": " + lpMetaStr + ". Options are " + String.join(", ", getOptionNames()));
                        provider = defaultProvider;
                    } else {
                        providerSource = ProviderSource.LUCK_PERMS_META;
                    }
                }
            }
        }

        Offset offset = provider.getOffset(context);

        if (plugin.getConfig().getBoolean("verbose")) {
            String reasonStr = null;
            switch (context.reason()) {
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

            plugin.getLogger().info(
                    "Using " + offset + " from provider \"" + provider.name + "\" (" + sourceStr + ") " +
                            "for player " + context.player().getName() + " in " + context.world().getName() + " (" + reasonStr + ").");
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
