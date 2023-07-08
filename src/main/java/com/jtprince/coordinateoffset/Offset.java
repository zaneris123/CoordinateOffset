package com.jtprince.coordinateoffset;

/**
 * Represents a coordinate offset in Overworld block space.
 * An offset of (16, 16) would result in a player seeing themselves at (0, 0) when they are standing at (16, 16) in the
 * Overworld, and seeing themselves standing at (-16, -16) when they are standing at the real origin.
 *
 * @param x Block X coordinate. Must be a multiple of 16 to align with chunk boundaries.
 * @param z Block Z coordinate. Must be a multiple of 16 to align with chunk boundaries.
 */
public record Offset (int x, int z) {
    public static final Offset ZERO = new Offset(0, 0);

    public Offset {
        if (x % 16 != 0) {
            throw new IllegalArgumentException("x is not aligned with the chunks!");
        }
        if (z % 16 != 0) {
            throw new IllegalArgumentException("z is not aligned with the chunks!");
        }
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }
}
