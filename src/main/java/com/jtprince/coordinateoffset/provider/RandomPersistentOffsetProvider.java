package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class RandomPersistentOffsetProvider extends OffsetProvider {
    private boolean alignOverworldAndNether;
    private boolean applyInEnd;
    private int randomBound;

    private NamespacedKey offsetKey;

    public RandomPersistentOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        Offset offset = context.player().getPersistentDataContainer().get(offsetKey, Offset.PDT_TYPE);
        if (offset == null) {
            offset = Offset.random(randomBound);
            context.player().getPersistentDataContainer().set(offsetKey, Offset.PDT_TYPE, offset);
        }

        if (alignOverworldAndNether && context.world().getEnvironment() == World.Environment.NETHER) {
            return offset.toNetherOffset();
        }
        if (!applyInEnd && context.world().getEnvironment() == World.Environment.THE_END) {
            return Offset.ZERO;
        }

        return offset;
    }

    public static class ConfigFactory implements ConfigurationFactory<RandomPersistentOffsetProvider> {
        @Override
        public @NotNull RandomPersistentOffsetProvider createProvider(String name, ConfigurationSection configSection) throws IllegalArgumentException {
            if (!configSection.isInt("randomBound")) {
                throw new IllegalArgumentException("Missing field randomBound for RandomPersistentOffsetProvider.");
            }

            RandomPersistentOffsetProvider p = new RandomPersistentOffsetProvider(name);
            p.alignOverworldAndNether = configSection.getBoolean("alignOverworldAndNether", true);
            p.applyInEnd = configSection.getBoolean("applyInEnd", true);
            // TODO: persistenceKey is not in the default yml
            p.offsetKey = new NamespacedKey(CoordinateOffset.getInstance(), "persistentoffset." + configSection.getString("persistenceKey", "default"));
            p.randomBound = configSection.getInt("randomBound");
            return p;
        }
    }
}
