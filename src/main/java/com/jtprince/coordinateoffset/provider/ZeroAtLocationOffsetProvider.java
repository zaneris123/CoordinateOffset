package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import com.jtprince.coordinateoffset.provider.util.Persistable;
import com.jtprince.coordinateoffset.provider.util.WorldAlignmentContainer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZeroAtLocationOffsetProvider extends OffsetProvider {
    private Persistable persistable;
    private WorldAlignmentContainer worldAlignment;
    private final Map<UUID, Map<String, Offset>> playerCache = new HashMap<>();

    private ZeroAtLocationOffsetProvider(String name) {
        super(name);
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        if (!playerCache.containsKey(context.player().getUniqueId())) {
            playerCache.put(context.player().getUniqueId(), new HashMap<>());
        }

        var thisPlayerCache = playerCache.get(context.player().getUniqueId());
        if (persistable.canPersist(context.reason()) && thisPlayerCache.containsKey(context.world().getName())) {
            return thisPlayerCache.get(context.world().getName());
        }

        Offset offset = null;

        // Check if we need to align to an offset we already generated for this player in another world
        if (persistable.canPersist(OffsetProviderContext.ProvideReason.WORLD_CHANGE)) {
            WorldAlignmentContainer.QueryResult alignment = worldAlignment.findAlignment(context.world().getName());
            if (alignment != null) {
                Offset alignedWorldOffset = thisPlayerCache.get(alignment.targetWorldName());
                if (alignedWorldOffset != null) {
                    offset = alignedWorldOffset.scale(alignment.rightShiftAmount());
                }
            }
        }

        // Generate a new offset if nothing else matched
        if (offset == null) {
            Location loc = context.playerLocation();
            int alignmentPower = worldAlignment.greatestPossibleRightShiftForWorld(context.world().getName());
            offset = Offset.align(loc.getBlockX(), loc.getBlockZ(), alignmentPower);
        }

        thisPlayerCache.put(context.world().getName(), offset);
        return offset;
    }

    @Override
    public void onPlayerQuit(@NotNull Player player) {
        playerCache.remove(player.getUniqueId());
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<ZeroAtLocationOffsetProvider> {
        @Override
        public @NotNull ZeroAtLocationOffsetProvider createProvider(String name, CoordinateOffset plugin, ConfigurationSection providerConfig) throws IllegalArgumentException {
            ZeroAtLocationOffsetProvider p = new ZeroAtLocationOffsetProvider(name);
            p.persistable = Persistable.fromConfigSection(providerConfig);
            p.worldAlignment = WorldAlignmentContainer.fromConfig(providerConfig.getStringList("worldAlignment"));
            return p;
        }
    }
}
