package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import com.jtprince.coordinateoffset.provider.util.Persistable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RandomOffsetProvider extends OffsetProvider {
    private Persistable persistable;
    private int randomBound;

    private final Map<UUID, Map<UUID, Offset>> playerCache = new HashMap<>();

    private RandomOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        if (!playerCache.containsKey(context.player().getUniqueId())) {
            playerCache.put(context.player().getUniqueId(), new HashMap<>());
        }

        var thisPlayerCache = playerCache.get(context.player().getUniqueId());
        if (persistable.canPersist(context.reason()) && thisPlayerCache.containsKey(context.world().getUID())) {
            return thisPlayerCache.get(context.world().getUID());
        }

        // Generate a new offset
        Offset offset = Offset.random(randomBound);
        thisPlayerCache.put(context.world().getUID(), offset);
        return offset;
    }

    @Override
    public void onPlayerQuit(@NotNull Player player) {
        playerCache.remove(player.getUniqueId());
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<RandomOffsetProvider> {
        @Override
        public @NotNull RandomOffsetProvider createProvider(String name, CoordinateOffset plugin, ConfigurationSection providerConfig) throws IllegalArgumentException {
            if (!providerConfig.isInt("randomBound")) {
                throw new IllegalArgumentException("Missing field randomBound for RandomOffsetProvider.");
            }

            RandomOffsetProvider p = new RandomOffsetProvider(name);
            p.persistable = Persistable.fromConfigSection(providerConfig);
            p.randomBound = providerConfig.getInt("randomBound");
            return p;
        }
    }
}
