package com.jtprince.coordinateoffset;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Container for relevant information when determining a new Offset.
 *
 * @param player The Player that will receive this new Offset.
 * @param world The World this Offset will apply to. Note that this may be different from <code>player.getWorld()</code>
 *              because the Provider is called <i>before</i> a world change.
 * @param playerLocation The real location that the Player will be at as soon as this Offset begins to take effect.
 *                       Note that this may be different from <code>player.getLocation()</code> because the Provider is
 *                       called <i>before</i> a teleport completes.
 * @param reason The reason that a new Offset is being requested.
 */
public record OffsetProviderContext(
    @NotNull Player player,
    @NotNull World world,
    @NotNull Location playerLocation,
    @NotNull ProvideReason reason
) {
    public enum ProvideReason {
        JOIN, RESPAWN, WORLD_CHANGE, DISTANT_TELEPORT
    }
}
