package com.jtprince.coordinateoffset;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConstantOffsetProvider extends OverworldOffsetProvider {
    final Offset overworldOffset;

    public ConstantOffsetProvider(Offset overworldOffset) {
        this.overworldOffset = overworldOffset;
    }

    @Override
    public @NotNull Offset getOverworldOffset(@NotNull Player player) {
        CoordinateOffset.instance.getLogger().info("Provided constant " + overworldOffset + " for player " + player.getName() + ".");
        return overworldOffset;
    }
}
