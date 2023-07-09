package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface OffsetProvider {
    /**
     * Generate a coordinate offset for a specific player in a world.
     *
     * <p>This function is called whenever the Offset has an opportunity to change. Currently, this occurs on player
     * login, respawn, world change, and distant teleport.</p>
     *
     * @param player The player who will receive this offset.
     * @param world The world that the player will be in.
     * @return The desired offset for this player.
     */
    @NotNull Offset getOffset(@NotNull Player player, @NotNull World world);
}
