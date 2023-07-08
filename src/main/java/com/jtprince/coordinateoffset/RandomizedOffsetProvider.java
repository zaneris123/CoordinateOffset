package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * An offset provider that randomizes each player's offset every time it is called.
 */
public class RandomizedOffsetProvider implements OffsetProvider {
    private final Random random = new Random();
    private static final int BOUND = 100000;

    @Override
    public @NotNull Offset getOffset(@NotNull Player player, @NotNull World world) {
        int x = random.nextInt(-BOUND, BOUND) >> 4 << 4;
        int z = random.nextInt(-BOUND, BOUND) >> 4 << 4;
        var offset = new Offset(x, z);
        CoordinateOffset.instance.getLogger().info("Provided random " + offset + " for player " + player.getName() + ".");
        return offset;
    }
}
