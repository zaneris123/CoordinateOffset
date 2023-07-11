package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract offset provider that simplifies Offset implementation for Vanilla-like servers by allowing the provider to
 * provide a single Offset for each player's Overworld rather than per-player AND per-world.
 *
 * <p>Each world's offset is derived from the provided Offset based on the {@link org.bukkit.World.Environment} of the
 * world:
 * <ul>
 *     <li>In NORMAL and CUSTOM environments, the offset returned by <code>getOverworldOffset</code> is used as-is.</li>
 *     <li>In NETHER environments, the offsets are divided by 8 to match the Vanilla scaling of the Nether.</li>
 *     <li>In THE_END environments, there is <b>no offset</b> because the End has a distinctive coordinate system
 *     centered on (0, 0).</li>
 * </ul></p>
 *
 * <p>For more flexibility in controlling the Offsets per-world, consider implementing {@link OffsetProvider}
 * instead.</p>
 */
public abstract class OverworldOffsetProvider extends OffsetProvider {
    public OverworldOffsetProvider(String name) {
        super(name);
    }

    @Override
    public final @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        switch (context.world().getEnvironment()) {
            case NORMAL, CUSTOM -> { return getOverworldOffset(context.player()); }
            case NETHER -> { return getOverworldOffset(context.player()).toNetherOffset(); }
            case THE_END -> { return Offset.ZERO; }
        }

        throw new IllegalArgumentException("Unknown world environment for world " + context.world().getName());
    }

    public abstract @NotNull Offset getOverworldOffset(@NotNull Player player);
}
