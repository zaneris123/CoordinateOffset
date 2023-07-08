package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class OverworldOffsetProvider implements OffsetProvider {
    @Override
    public final @NotNull Offset getOffset(@NotNull Player player, @NotNull World world) {
        switch (world.getEnvironment()) {
            case NORMAL, CUSTOM -> { return getOverworldOffset(player); }
            case NETHER -> { return getOverworldOffset(player).toNetherOffset(); }
            case THE_END -> { return Offset.ZERO; }
        }

        throw new IllegalArgumentException("Unknown world environment for world " + world.getName());
    }

    public abstract @NotNull Offset getOverworldOffset(@NotNull Player player);
}
