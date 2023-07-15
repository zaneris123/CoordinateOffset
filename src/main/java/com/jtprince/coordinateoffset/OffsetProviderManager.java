package com.jtprince.coordinateoffset;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class OffsetProviderManager {
    private final CoordinateOffset plugin;
    private final Map<String, OffsetProvider.ConfigurationFactory<?>> configFactories = new HashMap<>();
    private Map<String, OffsetProvider> providersFromConfig = new HashMap<>();

    private OffsetProvider defaultProvider;
    private List<ProviderOverride> overrides = Collections.emptyList();

    OffsetProviderManager(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

    private @NotNull Set<String> getOptionNames() {
        return providersFromConfig.keySet();
    }

    void registerConfigurationFactory(@NotNull String className, @NotNull OffsetProvider.ConfigurationFactory<?> factory) {
        configFactories.put(className, factory);
    }

    void loadProvidersFromConfig(@NotNull FileConfiguration config) throws IllegalArgumentException {
        ConfigurationSection providerSection = config.getConfigurationSection("offsetProviders");
        if (providerSection == null) {
            throw new IllegalArgumentException("Missing offsetProviders section from config!");
        }

        Map<String, OffsetProvider> newProviders = new HashMap<>();
        for (String providerName : providerSection.getKeys(false)) {
            ConfigurationSection section = providerSection.getConfigurationSection(providerName);
            if (section == null) {
                throw new IllegalArgumentException("Offset provider \"" + providerName + "\" is not a valid configuration section.");
            }
            String className = section.getString("class");
            if (className == null) {
                throw new IllegalArgumentException("Offset provider \"" + providerName + "\" is missing a \"class\" field.");
            }
            OffsetProvider.ConfigurationFactory<?> factory = configFactories.get(className);
            if (factory == null) {
                throw new IllegalArgumentException("Offset provider \"" + providerName + "\" has an unknown class name " + className + ".");
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
            throw new IllegalArgumentException("Unknown defaultOffsetProvider \"" + defaultProviderName + "\". Options are " + String.join(", ", getOptionNames()));
        }

        // Load overrides
        List<ProviderOverride> newOverrides = new ArrayList<>();
        List<Map<?, ?>> overridesSection = config.getMapList("offsetProviderOverrides");

        for (Map<?, ?> map : overridesSection) {
            Object providerName = map.get("provider");
            if (providerName == null) {
                throw new IllegalArgumentException("All overrides in offsetProviderOverrides must specify a \"provider\" key.");
            }
            OffsetProvider provider = providersFromConfig.get(providerName.toString());
            if (provider == null) {
                throw new IllegalArgumentException("Unknown provider \"" + providerName + "\" in offsetProviderOverrides. Options are " + String.join(", ", getOptionNames()));
            }

            UUID playerUuid = null;
            if (map.containsKey("playerUuid")) {
                playerUuid = UUID.fromString(map.get("playerUuid").toString()); // throws IllegalArgumentException
            }

            String worldName = null;
            if (map.containsKey("world")) {
                worldName = map.get("world").toString();
                if (Bukkit.getWorld(worldName) == null) {
                    plugin.getLogger().warning("Setting a provider override for unloaded world \"" + worldName + "\"!");
                }
            }

            newOverrides.add(new ProviderOverride(provider, playerUuid, worldName));
        }

        overrides = newOverrides;

        plugin.getLogger().info("Loaded " + newProviders.size() + " offset providers from config.");
        String overrideCountStr;
        switch (overrides.size()) {
            case 0 -> overrideCountStr = ".";
            case 1 -> overrideCountStr = " (+1 override rule).";
            default -> overrideCountStr = " (+" + overrides.size() + " override rules).";
        }
        plugin.getLogger().info("Default offset provider is \"" + defaultProvider.name + "\"" + overrideCountStr);
    }

    @NotNull Offset provideOffset(@NotNull OffsetProviderContext context) {
        OffsetProvider provider = defaultProvider;
        ProviderSource providerSource = ProviderSource.DEFAULT;

        if (plugin.getConfig().getBoolean("bypassByPermission") &&
                context.player().hasPermission(CoordinateOffsetPermissions.BYPASS)) {
            if (plugin.isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Bypassing offset with permission for player " + context.player().getName() + ".");
            }
            return Offset.ZERO;
        }

        Optional<ProviderOverride> appliedOverride = overrides.stream().filter(o -> o.appliesTo(context)).findFirst();
        if (appliedOverride.isPresent()) {
            provider = appliedOverride.get().provider;
            providerSource = ProviderSource.OVERRIDE;
        } else {
            LuckPermsIntegration lp = plugin.getLuckPermsIntegration();
            if (lp != null) {
                String lpMetaStr = lp.getProviderOverride(context.player(), context.world());
                if (lpMetaStr != null) {
                    provider = providersFromConfig.get(lpMetaStr);
                    if (provider == null) {
                        plugin.getLogger().severe("Unknown provider for LuckPerms meta lookup on " + context.player().getName() + ": \"" + lpMetaStr + "\". Options are " + String.join(", ", getOptionNames()));
                        provider = defaultProvider;
                    } else {
                        providerSource = ProviderSource.LUCK_PERMS_META;
                    }
                }
            }
        }

        Offset offset = provider.getOffset(context);

        if (plugin.isVerboseLoggingEnabled()) {
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
                case OVERRIDE -> sourceStr = "config.yml override";
                case LUCK_PERMS_META -> sourceStr = "LuckPerms meta override";
            }

            plugin.getLogger().info(
                    "Using " + offset + " from provider \"" + provider.name + "\" (" + sourceStr + ") " +
                            "for player " + context.player().getName() + " in world \"" + context.world().getName() + "\" (" + reasonStr + ").");
        }
        return offset;
    }

    enum ProviderSource {
        DEFAULT, OVERRIDE, LUCK_PERMS_META
    }

    record ProviderOverride(
        @NotNull OffsetProvider provider,
        @Nullable UUID playerUuid,
        @Nullable String worldName
    ) {
        @SuppressWarnings("RedundantIfStatement")
        boolean appliesTo(OffsetProviderContext context) {
            if (playerUuid != null && !playerUuid.equals(context.player().getUniqueId())) return false;
            if (worldName != null && !worldName.equals(context.world().getName())) return false;

            return true;
        }
    }

    void quitPlayer(@NotNull Player player) {
        for (OffsetProvider provider : providersFromConfig.values()) {
            provider.onPlayerQuit(player);
        }
    }
}
