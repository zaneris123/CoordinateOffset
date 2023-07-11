package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZeroOnJoinOffsetProvider extends OffsetProvider {
    private boolean alignOverworldAndNether;

    private final Map<UUID, Map<UUID, Offset>> playerCache = new HashMap<>();
    private final Map<UUID, UUID> lastKnownPlayerOverworld = new HashMap<>();
    private final Map<UUID, UUID> lastKnownPlayerNether = new HashMap<>();

    public ZeroOnJoinOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        if (!playerCache.containsKey(context.player().getUniqueId())) {
            playerCache.put(context.player().getUniqueId(), new HashMap<>());
        }

        switch (context.world().getEnvironment()) {
            case NORMAL -> lastKnownPlayerOverworld.put(context.player().getUniqueId(), context.world().getUID());
            case NETHER -> lastKnownPlayerNether.put(context.player().getUniqueId(), context.world().getUID());
        }

        var thisPlayerCache = playerCache.get(context.player().getUniqueId());
        if (thisPlayerCache.containsKey(context.world().getUID())) {
            return thisPlayerCache.get(context.world().getUID());
        }

        /*
         * Generate a new offset. If dimensions are aligned, check if we already have an offset for the complementary
         * dimension to scale and reuse.
         */
        Offset offset = null;
        if (alignOverworldAndNether) {
            switch (context.world().getEnvironment()) {
                case NORMAL -> {
                    UUID lastNetherUuid = lastKnownPlayerNether.get(context.player().getUniqueId());
                    if (lastNetherUuid == null) break;
                    offset = thisPlayerCache.get(lastNetherUuid);
                    if (offset != null) offset = offset.toOverworldFromNetherOffset();
                }
                case NETHER -> {
                    UUID lastOverworldUuid = lastKnownPlayerOverworld.get(context.player().getUniqueId());
                    if (lastOverworldUuid == null) break;
                    offset = thisPlayerCache.get(lastOverworldUuid);
                    if (offset != null) offset = offset.toNetherOffset();
                }
            }
        }

        if (offset == null) {
            Location loc = context.playerLocation();
            offset = Offset.align(loc.getBlockX(), loc.getBlockZ(), context.world().getEnvironment() != World.Environment.NETHER);
        }

        thisPlayerCache.put(context.world().getUID(), offset);
        return offset;
    }

    @Override
    public void onPlayerQuit(@NotNull Player player) {
        playerCache.remove(player.getUniqueId());
        lastKnownPlayerOverworld.remove(player.getUniqueId());
        lastKnownPlayerNether.remove(player.getUniqueId());
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<ZeroOnJoinOffsetProvider> {
        @Override
        public @NotNull ZeroOnJoinOffsetProvider createProvider(String name, ConfigurationSection configSection) throws IllegalArgumentException {
            ZeroOnJoinOffsetProvider p = new ZeroOnJoinOffsetProvider(name);
            p.alignOverworldAndNether = configSection.getBoolean("alignOverworldAndNether");
            return p;
        }
    }
}
