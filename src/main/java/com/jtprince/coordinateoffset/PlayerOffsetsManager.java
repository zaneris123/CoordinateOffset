package com.jtprince.coordinateoffset;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public class PlayerOffsetsManager {
    private final Map<UUID, Offset> playerOffsets = new HashMap<>();
    private final Map<UUID, UUID> playerKnownWorlds = new HashMap<>();

    /*
     * TODO: The expectedWorld argument can be dropped, as it just provides validation that the caller knows which
     *  world this Offset applies to.
     */
    public synchronized @NotNull Offset get(@NotNull Player player, @Nullable World expectedWorld) {
        Offset offset = playerOffsets.get(player.getUniqueId());
        if (offset == null) {
            throw new NoSuchElementException("Unknown player for Offset lookup: " + player.getName());
        }

        if (expectedWorld != null) {
            UUID knownWorld = playerKnownWorlds.get(player.getUniqueId());
            if (expectedWorld.getUID() != knownWorld) {
                CoordinateOffset.getInstance().getLogger().severe("Mismatched world for provided offsets! (" + expectedWorld.getName() + ")");
            }
        }

        return offset;
    }

    synchronized void put(@NotNull Player player, @NotNull World world, @NotNull Offset offset) {
        playerOffsets.put(player.getUniqueId(), offset);
        playerKnownWorlds.put(player.getUniqueId(), world.getUID());
    }

    synchronized void remove(@NotNull Player player) {
        playerOffsets.remove(player.getUniqueId());
        playerKnownWorlds.remove(player.getUniqueId());
    }
}
