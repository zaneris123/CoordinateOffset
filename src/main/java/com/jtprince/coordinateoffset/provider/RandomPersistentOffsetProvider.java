package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import com.jtprince.coordinateoffset.provider.util.WorldAlignmentContainer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class RandomPersistentOffsetProvider extends OffsetProvider {
    private static final String DEFAULT_PERSISTENCE_KEY = "default";
    private String persistenceKey;
    private int randomBound;
    private WorldAlignmentContainer worldAlignment;

    private RandomPersistentOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        NamespacedKey key = containerKey(context.plugin(), context.world().getName());

        Offset offset = context.player().getPersistentDataContainer().get(key, Offset.PDT_TYPE);
        if (offset != null) {
            return offset;
        }

        // Check if we need to align to an offset we already generated for this player in another world
        WorldAlignmentContainer.QueryResult alignment = worldAlignment.findAlignment(context.world().getName());
        if (alignment != null) {
            NamespacedKey alignedWorldOffsetKey = containerKey(context.plugin(), alignment.targetWorldName());
            Offset alignedWorldOffset = context.player().getPersistentDataContainer().get(alignedWorldOffsetKey, Offset.PDT_TYPE);
            if (alignedWorldOffset != null) {
                offset = alignedWorldOffset.scale(alignment.rightShiftAmount());
                if (context.plugin().isVerboseLoggingEnabled()) {
                    String scaleStr;
                    if (alignment.rightShiftAmount() == 0) scaleStr = ".";
                    else if (alignment.rightShiftAmount() < 0) scaleStr = " (scaled up by " + (1 << -alignment.rightShiftAmount()) + ").";
                    else scaleStr = " (scaled down by " + (1 << alignment.rightShiftAmount()) + ").";
                    context.plugin().getLogger().info("Provider \"" + name + "\": Aligning new offset for world \"" +
                            context.world().getName() + "\" to existing " + alignedWorldOffset + " from world \"" +
                            alignment.targetWorldName() + "\"" + scaleStr);
                }
            }
        }

        // Generate a new offset if nothing else matched
        if (offset == null) {
            offset = Offset.random(randomBound);
        }

        context.player().getPersistentDataContainer().set(key, Offset.PDT_TYPE, offset);
        return offset;
    }

    private NamespacedKey containerKey(CoordinateOffset plugin, String worldName) {
        return new NamespacedKey(plugin, "random-persistent." + persistenceKey + "." + worldName);
    }

    public static class ConfigFactory implements ConfigurationFactory<RandomPersistentOffsetProvider> {
        @Override
        public @NotNull RandomPersistentOffsetProvider createProvider(String name, CoordinateOffset plugin, ConfigurationSection providerConfig) throws IllegalArgumentException {
            if (!providerConfig.isInt("randomBound")) {
                throw new IllegalArgumentException("Missing field randomBound for RandomPersistentOffsetProvider.");
            }

            RandomPersistentOffsetProvider p = new RandomPersistentOffsetProvider(name);
            // TODO: persistenceKey is not in the default yml
            p.persistenceKey = providerConfig.getString("persistenceKey", DEFAULT_PERSISTENCE_KEY);
            p.randomBound = providerConfig.getInt("randomBound");
            p.worldAlignment = WorldAlignmentContainer.fromConfig(providerConfig.getStringList("worldAlignment"));
            return p;
        }
    }
}
