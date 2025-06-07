package com.jtprince.coordinateoffset;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;

/**
 * Ensures the offset cache is initialized as early as possible (PlayerLoginEvent).
 * This is critical for Folia, where packet events may fire before later join events.
 */
public class LoginListener implements Listener {
    private final CoordinateOffset plugin;
    private final PlayerOffsetsManager playerOffsetsManager;

    public LoginListener(CoordinateOffset plugin, PlayerOffsetsManager playerOffsetsManager) {
        this.plugin = plugin;
        this.playerOffsetsManager = playerOffsetsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        // Defensive: Only initialize if not already present
        try {
            playerOffsetsManager.getOffset(player);
        } catch (Exception e) {
            // No offset yet, so initialize for the player's current world
            OffsetProviderContext context = new OffsetProviderContext(
                player,
                player.getWorld(),
                player.getLocation(),
                OffsetProviderContext.ProvideReason.JOIN,
                plugin
            );
            playerOffsetsManager.regenerateOffset(context);
            if (plugin.isVerboseLoggingEnabled()) {
                plugin.getLogger().info("[LoginListener] Initialized offset cache for " + player.getName() + " on login.");
            }
        }
    }
}
