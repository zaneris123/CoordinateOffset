package com.jtprince.coordinateoffset;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.morepersistentdatatypes.datatypes.GenericDataType;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Represents a coordinate offset in block space.
 *
 * <p>An offset of (16, 16) would result in a player seeing themselves at (0, 0) when they are standing at (16, 16) in
 * the Overworld, and seeing themselves standing at (-16, -16) when they are standing at the real origin.</p>
 *
 * @param x Block X coordinate. Must be a multiple of 16 to align with chunk boundaries.
 * @param z Block Z coordinate. Must be a multiple of 16 to align with chunk boundaries.
 */
public record Offset (int x, int z) {
    public static final Offset ZERO = new Offset(0, 0);

    public static final PersistentDataType<int[], Offset> PDT_TYPE =
            new GenericDataType<>(DataType.INTEGER_ARRAY.getPrimitiveType(), Offset.class, Offset::fromPdt, Offset::toPdt);

    public Offset {
        if (x % 16 != 0) {
            throw new IllegalArgumentException("Offset x=" + x + " is not aligned with the chunks! (must be a multiple of 16)");
        }
        if (z % 16 != 0) {
            throw new IllegalArgumentException("Offset z=" + z + " is not aligned with the chunks! (must be a multiple of 16)");
        }
    }

    /**
     * Get a random Offset, with x and z in the range <code>(-bound, bound)</code>.
     * @param bound Maximum absolute value of each offset dimension.
     * @return A new Offset aligned to 128-blocks.
     */
    public static @NotNull Offset random(int bound) {
        Random random = new Random();
        return align(random.nextInt(-bound, bound), random.nextInt(-bound, bound), true);
    }

    /**
     * Get a new Offset closest to the specified offset that is aligned to chunk borders.
     *
     * <p>Offsets MUST be aligned with chunk borders, meaning each dimension is divisible by 16.</p>
     * @param x X offset
     * @param z Z offset
     * @param to8Chunks To make Nether translations more predictable, Overworld offsets should also be divisible by 16
     *                  even after dividing once by 8. If this parameter is true, the result will be aligned to
     *                  128-blocks; if false, the result will be aligned only to 16-blocks.
     * @return A new Offset.
     */
    public static @NotNull Offset align(int x, int z, boolean to8Chunks) {
        int shift = to8Chunks ? 7 : 4;

        // Add half of the divisor so that the output is rounded instead of just floored
        x += 1 << (shift - 1);
        z += 1 << (shift - 1);

        return new Offset(x >> shift << shift, z >> shift << shift);
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    /**
     * Get an equivalent offset in the Nether for this Offset, assuming that this is an Overworld Offset, by dividing
     * the values by 8.
     *
     * @return A new Offset with coordinates divided by 8 and rounded to align with chunk boundaries.
     */
    @Pure
    public Offset toNetherOffset() {
        return new Offset(x >> 7 << 4, z >> 7 << 4);
    }

    /**
     * Get an equivalent offset in the Overworld for this Offset, assuming that this is a Nether Offset, by multiplying
     * the values by 8.
     *
     * @return A new Offset with coordinates multiplied by 8.
     */
    @Pure
    public Offset toOverworldFromNetherOffset() {
        return new Offset(x << 3, z << 3);
    }

    /**
     * Apply this Offset to a Bukkit Location, resulting in the Location that a player who has this Offset would see
     * if they were at that Location.
     *
     * <p>Care should be taken not to use the returned Location for anything internal to the server, such as getting the
     * Block at that Location. The returned Location is primarily intended to be sent to a Player who this Offset is
     * applied to, such as in a message.</p>
     *
     * @param realLocation A Location on the server, in real coordinate space.
     * @return A new Location object that represents the coordinates that the player will see.
     */
    @Pure
    public Location offsetted(Location realLocation) {
        if (realLocation == null) return null;
        return realLocation.clone().subtract(this.x, 0, this.z);
    }

    /**
     * Apply this Offset to a ProtocolLib BlockPosition, resulting in the position that a player who has this Offset
     * would see if they were at that position.
     *
     * <p>Care should be taken not to use the returned BlockPosition for anything internal to the server. The
     * returned BlockPosition is primarily intended to be sent to a Player who this Offset is applied to.</p>
     *
     * @param realPosition A BlockPosition on the server, in real coordinate space.
     * @return A new BlockPosition object that represents the coordinates that the player will see.
     */
    @Pure
    public BlockPosition offsetted(BlockPosition realPosition) {
        if (realPosition == null) return null;
        return realPosition.subtract(new BlockPosition(this.x, 0, this.z));
    }

    /**
     * Apply the inverse of this Offset to a Bukkit Location, resulting in a real server Location.
     *
     * @param offsettedLocation An offsetted Location coming from a Player who has this offset.
     * @return A new Location object that represents the real Location for the server to use.
     */
    @Pure
    public Location unoffsetted(Location offsettedLocation) {
        if (offsettedLocation == null) return null;
        return offsettedLocation.clone().add(this.x, 0, this.z);
    }

    /**
     * Apply the inverse of this Offset to a ProtocolLib BlockPosition, resulting in a real server position.
     *
     * @param offsettedPosition An offsetted BlockPosition coming from a Player who has this offset.
     * @return A new BlockPosition object that represents the real BlockPosition for the server to use.
     */
    @Pure
    public BlockPosition unoffsetted(BlockPosition offsettedPosition) {
        if (offsettedPosition == null) return null;
        return offsettedPosition.add(new BlockPosition(this.x, 0, this.z));
    }

    private static Offset fromPdt(int[] arr) {
        return new Offset(arr[0], arr[1]);
    }

    private int[] toPdt() {
        return new int[] { x, z };
    }
}
