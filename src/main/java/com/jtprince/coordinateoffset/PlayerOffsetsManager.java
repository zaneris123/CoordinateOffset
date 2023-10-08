package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Container for the offset a player currently has.
 */
class PlayerOffsetsManager {
    private final CoordinateOffset plugin;

    private final Map<UUID, Map<UUID, Offset>> playerOffsets = new HashMap<>();

    /* TODO Comment why there are 2 */
    private final Map<UUID, UUID> playerRespawningWorld = new HashMap<>();
    private final Map<UUID, UUID> playerPositionedWorld = new HashMap<>();

    PlayerOffsetsManager(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

    synchronized @NotNull Offset get(@NotNull Player player) {
        Map<UUID, Offset> offsetPerWorldCache = getPerWorldCacheFor(player);

        UUID positionedWorld = playerPositionedWorld.get(player.getUniqueId());
        if (positionedWorld == null) {
            throw new NoSuchElementException("Can't determine which world player is in: " + player.getName());
        }

        return offsetPerWorldCache.get(positionedWorld);
    }

    synchronized @NotNull Offset get(@NotNull Player player, @NotNull World world) {
        Map<UUID, Offset> offsetPerWorldCache = getPerWorldCacheFor(player);
        return offsetPerWorldCache.get(world.getUID());
    }

    synchronized @NotNull Offset getRespawning(@NotNull Player player) {
        Map<UUID, Offset> offsetPerWorldCache = getPerWorldCacheFor(player);
        UUID respawningWorld = playerRespawningWorld.get(player.getUniqueId());
        if (respawningWorld == null) {
            throw new NoSuchElementException("Can't determine which world player is in: " + player.getName());
        }

        return offsetPerWorldCache.get(respawningWorld);
    }

    private @NotNull Map<UUID, Offset> getPerWorldCacheFor(Player player) {
        Map<UUID, Offset> offsetPerWorldCache = playerOffsets.get(player.getUniqueId());
        if (offsetPerWorldCache == null) {
            throw new NoSuchElementException("Unknown player for Offset lookup: " + player.getName());
        }
        return offsetPerWorldCache;
    }

    synchronized void regenerateOffset(OffsetProviderContext context) {
        Offset newOffset = plugin.getOffsetProviderManager().provideOffset(context);
        Map<UUID, Offset> offsetPerWorldCache = playerOffsets.computeIfAbsent(context.player().getUniqueId(), k -> new HashMap<>());
        offsetPerWorldCache.put(context.world().getUID(), newOffset);
    }

    synchronized void setPositionedWorld(Player player, World world) {
        playerPositionedWorld.put(player.getUniqueId(), world.getUID());
    }

    synchronized void setRespawningWorld(Player player, World world) {
        playerRespawningWorld.put(player.getUniqueId(), world.getUID());
    }

    synchronized void remove(@NotNull Player player) {
        playerOffsets.remove(player.getUniqueId());
        playerRespawningWorld.remove(player.getUniqueId());
        playerPositionedWorld.remove(player.getUniqueId());
    }
}
