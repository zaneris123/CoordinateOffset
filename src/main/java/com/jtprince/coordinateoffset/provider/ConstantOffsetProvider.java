package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConstantOffsetProvider extends OverworldOffsetProvider {
    final Offset overworldOffset;

    public ConstantOffsetProvider(Offset overworldOffset) {
        this.overworldOffset = overworldOffset;
    }

    @Override
    public @NotNull Offset getOverworldOffset(@NotNull Player player) {
        return overworldOffset;
    }
}
