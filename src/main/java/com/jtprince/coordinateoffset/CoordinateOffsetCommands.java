package com.jtprince.coordinateoffset;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
class CoordinateOffsetCommands {
    private final CoordinateOffset plugin;
    private final ComponentBuilder prefix = new ComponentBuilder("[CoordinateOffset] ").color(ChatColor.AQUA);

    public CoordinateOffsetCommands(CoordinateOffset plugin) {
        this.plugin = plugin;
    }

    void registerCommands() {
        new CommandAPICommand("offset")
                .withPermission(CoordinateOffsetPermissions.QUERY_SELF)
                .executesPlayer((player, args) -> {
                    Offset offset = plugin.getPlayerManager().get(player, player.getWorld());
                    player.spigot().sendMessage(new ComponentBuilder(prefix)
                            .append("[x=" + offset.x() + ", z=" + offset.z() + "]").color(ChatColor.GOLD)
                            .append(" is your current offset.").color(ChatColor.GREEN)
                            .create());

                    Location real = player.getLocation();
                    player.spigot().sendMessage(new ComponentBuilder(prefix)
                            .append(String.format("(%.1f, %.1f, %.1f)", real.getX(), real.getY(), real.getZ())).color(ChatColor.YELLOW)
                            .append(" is your real position in world ").color(ChatColor.GREEN)
                            .append(player.getWorld().getName()).color(ChatColor.YELLOW)
                            .append(".").color(ChatColor.GREEN)
                            .create());
                })
                .register();

        new CommandAPICommand("offset")
                .withArguments(new PlayerArgument("player").withPermission(CoordinateOffsetPermissions.QUERY_OTHERS))
                .executes((sender, args) -> {
                    Player player = (Player) args.get("player");
                    if (player == null) {
                        replyError(sender, "Unknown player.");
                        return;
                    }
                    Offset offset = plugin.getPlayerManager().get(player, player.getWorld());
                    sender.spigot().sendMessage(new ComponentBuilder(prefix)
                            .append("[x=" + offset.x() + ", z=" + offset.z() + "]").color(ChatColor.GOLD)
                            .append(" is " + player.getName() + "'s current offset.").color(ChatColor.GREEN)
                            .create());

                    Location real = player.getLocation();
                    sender.spigot().sendMessage(new ComponentBuilder(prefix)
                            .append(String.format("(%.1f, %.1f, %.1f)", real.getX(), real.getY(), real.getZ())).color(ChatColor.YELLOW)
                            .append(" is their real position in world ").color(ChatColor.GREEN)
                            .append(player.getWorld().getName()).color(ChatColor.YELLOW)
                            .append(".").color(ChatColor.GREEN)
                            .create());
                })
                .register();

        new CommandAPICommand("offsetreload")
                .withPermission(CoordinateOffsetPermissions.RELOAD)
                .executes((sender, args) -> {
                    try {
                        plugin.reload();
                        replyOk(sender, "Reloaded CoordinateOffset config. Players may need to relog to see the changes.");
                    } catch (Exception e) {
                        replyError(sender, "Failed to reload the config. Check the console for details.");
                        e.printStackTrace();
                    }
                })
                .register();
    }

    private void replyOk(CommandSender to, @SuppressWarnings("SameParameterValue") String message) {
        to.spigot().sendMessage(new ComponentBuilder(prefix).append(message).color(ChatColor.GREEN).create());
    }

    private void replyError(CommandSender to, String message) {
        to.spigot().sendMessage(new ComponentBuilder(prefix).append(message).color(ChatColor.RED).create());
    }
}
