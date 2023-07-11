package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RandomOffsetProvider extends OffsetProvider {
    private boolean persistAcrossRespawns;
    private boolean persistAcrossWorldChanges;
    private boolean persistAcrossDistantTeleports;
    private int randomBound;

    private final Map<UUID, Map<UUID, Offset>> playerCache = new HashMap<>();

    public RandomOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        if (!playerCache.containsKey(context.player().getUniqueId())) {
            playerCache.put(context.player().getUniqueId(), new HashMap<>());
        }

        var thisPlayerCache = playerCache.get(context.player().getUniqueId());
        if (canPersist(context.reason()) && thisPlayerCache.containsKey(context.world().getUID())) {
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

    private boolean canPersist(OffsetProviderContext.ProvideReason reason) {
        switch (reason) {
            case RESPAWN -> { return persistAcrossRespawns; }
            case WORLD_CHANGE -> { return persistAcrossWorldChanges; }
            case DISTANT_TELEPORT -> { return persistAcrossDistantTeleports; }
            default -> { return false; }
        }
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<RandomOffsetProvider> {
        @Override
        public @NotNull RandomOffsetProvider createProvider(String name, ConfigurationSection configSection) throws IllegalArgumentException {
            if (!configSection.isInt("randomBound")) {
                throw new IllegalArgumentException("Missing field randomBound for RandomOffsetProvider.");
            }

            RandomOffsetProvider p = new RandomOffsetProvider(name);
            p.persistAcrossRespawns = configSection.getBoolean("persistAcrossRespawns");
            p.persistAcrossWorldChanges = configSection.getBoolean("persistAcrossWorldChanges");
            p.persistAcrossDistantTeleports = configSection.getBoolean("persistAcrossDistantTeleports");
            p.randomBound = configSection.getInt("randomBound");
            return p;
        }
    }
}
