package com.jtprince.coordinateoffset;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * World border packets require special handling, since applying a plain offset would run into two problems:
 * <ul>
 *     <li>The player can intercept the packet and use it to derive their offset.</li>
 *     <li>World border coordinates apply world scaling for some reason (the only packets that seem to...)</li>
 * </ul>
 * <a href="https://github.com/joshuaprince/CoordinateOffset/wiki/Implications-and-Limitations#world-border">Wiki</a>
 */
class WorldBorderObfuscator {
    private static final double BASELINE_SIZE = 60_000_000;

    private final CoordinateOffset plugin;
    private final Map<UUID, EnumSet<Wall>> knownSeenWalls = new HashMap<>();

    WorldBorderObfuscator(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

    void tryUpdatePlayerBorders(Player player, Location movingTo) {
        EnumSet<Wall> currentlyVisible = visibleBorders(movingTo);
        if (!currentlyVisible.equals(knownSeenWalls.get(player.getUniqueId()))) {
            plugin.getLogger().fine("Seen walls update for " + player.getName() + ": " + currentlyVisible);
            knownSeenWalls.put(player.getUniqueId(), currentlyVisible);
            updateBorderObfuscation(player);
        }
    }

    void onPlayerQuit(Player player) {
        knownSeenWalls.remove(player.getUniqueId());
    }

    private boolean enableObfuscation() {
        return plugin.getConfig().getBoolean("obfuscateWorldBorder");
    }

    private EnumSet<Wall> visibleBorders(Location location) {
        double viewDistanceBlocks = Objects.requireNonNull(location.getWorld()).getViewDistance() * 16;

        WorldBorder realBorder = location.getWorld().getWorldBorder();
        double xMax = realBorder.getCenter().getX() + realBorder.getSize() / 2;
        double xMin = realBorder.getCenter().getX() - realBorder.getSize() / 2;
        double zMax = realBorder.getCenter().getZ() + realBorder.getSize() / 2;
        double zMin = realBorder.getCenter().getZ() - realBorder.getSize() / 2;

        EnumSet<Wall> seen = EnumSet.noneOf(Wall.class);
        if (xMax - location.getX() < viewDistanceBlocks) {
            seen.add(Wall.X_POSITIVE);
        }
        if (location.getX() - xMin < viewDistanceBlocks) {
            seen.add(Wall.X_NEGATIVE);
        }
        if (zMax - location.getZ() < viewDistanceBlocks) {
            seen.add(Wall.Z_POSITIVE);
        }
        if (location.getZ() - zMin < viewDistanceBlocks) {
            seen.add(Wall.Z_NEGATIVE);
        }

        return seen;
    }

    private void updateBorderObfuscation(Player player) {
        // Force-send the player border packets that we will then translate.
        // Online check is necessary so that we don't send PLAY-phase packets during player login. (GitHub issue #5)
        if (player.isOnline() && enableObfuscation()) {
            player.setWorldBorder(player.getWorldBorder());
        }
    }

    void translate(@NotNull PacketContainer packet, @NotNull Player player) {
        Offset offset = plugin.getPlayerManager().get(player);

        /*
         * For reasons I cannot fathom, the Minecraft protocol applies the world's coordinate scaling to the world
         * border center location. (e.g. if I wanted to center a border at Nether coordinates (100,100), I would need to
         * send a packet containing (800, 800) as the center.)
         *
         * This could cause problems if the server is running a custom world with a different coordinateScale (which is
         * only accessible through NMS as DimensionType::coordinateScale). For now, just checking environment should be
         * enough.
         */
        double scaleFactor;
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            scaleFactor = 8.0;
        } else {
            scaleFactor = 1.0;
        }

        EnumSet<Wall> seenWalls = knownSeenWalls.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Wall.class));
        if (!enableObfuscation() ||
                (seenWalls.contains(Wall.X_POSITIVE) && seenWalls.contains(Wall.X_NEGATIVE)) ||
                (seenWalls.contains(Wall.Z_POSITIVE) && seenWalls.contains(Wall.Z_NEGATIVE))) {
            // If the player can see opposing walls, or obfuscation is disabled, we should just send the complete
            // offsetted border. No diameter change.
            switch (packet.getType().name()) {
                case "INITIALIZE_BORDER", "SET_BORDER_CENTER" -> {
                    packet.getDoubles().modify(0, x -> x - (offset.x() * scaleFactor));
                    packet.getDoubles().modify(1, z -> z - (offset.z() * scaleFactor));
                }
            }
            return;
        }

        WorldBorder border = player.getWorldBorder();
        if (border == null) border = player.getWorld().getWorldBorder();

        double centerX = 0.0, centerZ = 0.0;
        final double diameter = BASELINE_SIZE;

        if (seenWalls.size() >= 1) {
            // The player can see one wall, or two walls that are on different axes, adjust the border such that the
            // walls they can't see are a constant and large distance away.
            if (seenWalls.contains(Wall.X_POSITIVE)) {
                double realXMax = border.getCenter().getX() + border.getSize() / 2;
                centerX = realXMax - (BASELINE_SIZE / 2);
                centerX -= offset.x();
            }
            if (seenWalls.contains(Wall.X_NEGATIVE)) {
                double realXMin = border.getCenter().getX() - border.getSize() / 2;
                centerX = realXMin + (BASELINE_SIZE / 2);
                centerX -= offset.x();
            }
            if (seenWalls.contains(Wall.Z_POSITIVE)) {
                double realZMax = border.getCenter().getZ() + border.getSize() / 2;
                centerZ = realZMax - (BASELINE_SIZE / 2);
                centerZ -= offset.z();
            }
            if (seenWalls.contains(Wall.Z_NEGATIVE)) {
                double realZMin = border.getCenter().getZ() - border.getSize() / 2;
                centerZ = realZMin + (BASELINE_SIZE / 2);
                centerZ -= offset.z();
            }
        } else {
            // The player cannot see any walls. Fully obfuscate the worldborder.
            centerX = centerZ = 0.0;
        }

        switch (packet.getType().name()) {
            case "INITIALIZE_BORDER" -> {
                packet.getDoubles().write(0, centerX * scaleFactor);
                packet.getDoubles().write(1, centerZ * scaleFactor);
                packet.getDoubles().write(2, diameter);
                packet.getDoubles().write(3, diameter);
            }
            case "SET_BORDER_CENTER" -> {
                packet.getDoubles().write(0, centerX * scaleFactor);
                packet.getDoubles().write(1, centerZ * scaleFactor);
            }
            case "SET_BORDER_LERP_SIZE" -> {
                packet.getDoubles().write(0, diameter);
                packet.getDoubles().write(1, diameter);
            }
            case "SET_BORDER_SIZE" -> packet.getDoubles().write(0, diameter);
        }
    }

    enum Wall {
        X_POSITIVE,
        X_NEGATIVE,
        Z_POSITIVE,
        Z_NEGATIVE
    }
}
