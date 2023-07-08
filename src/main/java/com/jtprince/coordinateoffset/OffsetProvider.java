package com.jtprince.coordinateoffset;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface OffsetProvider {
    /**
     * Generate a coordinate offset for a specific Player.
     * This function is called whenever the Offset has an opportunity to change. Currently, this occurs on player login,
     * respawn, and world change. TODO!
     *
     * @param player The player who will receive this offset.
     * @return The desired offset for this player.
     */
    @NotNull Offset getOffset(@NotNull Player player);
}
