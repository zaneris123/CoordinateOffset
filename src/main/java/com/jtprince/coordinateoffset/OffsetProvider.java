package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Offset Providers are an extensible way to determine what offset from real (server) coordinates each player should
 * get in each world.
 */
public abstract class OffsetProvider {
    public enum ProvideReason {
        JOIN, RESPAWN, WORLD_CHANGE, DISTANT_TELEPORT
    }

    public final String name;

    public OffsetProvider(String name) {
        this.name = name;
    }

    /**
     * Generate a coordinate offset for a specific player in a world.
     *
     * <p>This function is called whenever the Offset has an opportunity to change. The reasons that the Offset might
     * be changing are enumerated in {@link ProvideReason}.</p>
     *
     * @param player The player who will receive this offset.
     * @param world The world that the player will be in.
     * @param reason The reason that this player's Offset has an opportunity to change.
     * @return The desired offset for this player.
     */
    public abstract @NotNull Offset getOffset(@NotNull Player player, @NotNull World world, @NotNull ProvideReason reason);

    /**
     * Called on this provider whenever a player leaves. The provider should clean up or write to disk any saved state
     * related to that player.
     *
     * <p>The implementer should NOT assume that {@link #getOffset} has been called with this Player at any point before
     * this.</p>
     * @param player The player that's leaving.
     */
    public void onPlayerQuit(@NotNull Player player) {}

    /**
     * An OffsetProvider.ConfigurationFactory encodes how the CoordinateOffset plugin should create providers as defined
     * in config.yml.
     * @param <T> The type of OffsetProvider that this factory will create.
     */
    public interface ConfigurationFactory<T extends OffsetProvider> {
        @NotNull T createProvider(String name, ConfigurationSection configSection) throws IllegalArgumentException;
    }
}
