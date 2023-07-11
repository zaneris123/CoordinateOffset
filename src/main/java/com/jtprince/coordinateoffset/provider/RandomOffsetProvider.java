package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import org.bukkit.World;
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
    public @NotNull Offset getOffset(@NotNull Player player, @NotNull World world, @NotNull ProvideReason reason) {
        if (!playerCache.containsKey(player.getUniqueId())) {
            playerCache.put(player.getUniqueId(), new HashMap<>());
        }

        var thisPlayerCache = playerCache.get(player.getUniqueId());
        if (canPersist(reason) && thisPlayerCache.containsKey(world.getUID())) {
            return thisPlayerCache.get(world.getUID());
        }

        // Generate a new offset
        Offset offset = Offset.random(randomBound);
        thisPlayerCache.put(world.getUID(), offset);
        return offset;
    }

    @Override
    public void onPlayerQuit(@NotNull Player player) {
        playerCache.remove(player.getUniqueId());
    }

    private boolean canPersist(ProvideReason reason) {
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
