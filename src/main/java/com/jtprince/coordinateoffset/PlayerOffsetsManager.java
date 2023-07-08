package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public class PlayerOffsetsManager {
    private final Map<UUID, Map<UUID, Offset>> playerOffsetsPerWorld = new HashMap<>();

    public synchronized @NotNull Offset get(@NotNull Player player, @NotNull World world) {
        var forPlayer = playerOffsetsPerWorld.get(player.getUniqueId());
        if (forPlayer == null) {
            throw new NoSuchElementException("Unknown player for Offset lookup: " + player.getName());
        }

        var offset = forPlayer.get(world.getUID());
        if (offset == null) {
            throw new NoSuchElementException("Unknown world for Offset lookup on " + player.getName() + ": " + world.getName());
        }

        return offset;
    }

    synchronized void put(@NotNull Player player, @NotNull World world, @NotNull Offset offset) {
        if (!playerOffsetsPerWorld.containsKey(player.getUniqueId())) {
            playerOffsetsPerWorld.put(player.getUniqueId(), new HashMap<>());
        }
        playerOffsetsPerWorld.get(player.getUniqueId()).put(world.getUID(), offset);
    }
}
