package com.jtprince.coordinateoffset;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuckPermsIntegration {
    private static final String META_KEY_PROVIDER_OVERRIDE = "coordinate-offset-provider";
    private final LuckPerms luckPerms;
    private final PlayerAdapter<Player> adapter;

    public LuckPermsIntegration(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
        adapter = luckPerms.getPlayerAdapter(Player.class);
    }

    public @Nullable String getProviderOverride(@NotNull Player player, @NotNull World world) {
        /*
         * A simple permission lookup does not work because LuckPerms updates its permission context AFTER a player
         * teleports between worlds. To make sure we're getting the correct meta for the world the player might be
         * teleporting to, we have to do a manual permission lookup with the target world in the context.
         */
        QueryOptions query = adapter.getUser(player).getQueryOptions();
        MutableContextSet context = query.context().mutableCopy();

        CachedMetaData cache;
        if (context.isEmpty()) {
            /*
             * When the player first joins, the query context is empty because we generate an offset in the LOGIN
             * packet, but LuckPerms doesn't load in player data until later in the PlayerJoinEvent.
             */
            context.add("world", world.getName());
            context.add("gamemode", getLPGameModeName(player.getGameMode()));
            context.add("dimension-type", getLPEnvironmentName(world.getEnvironment()));
            context.add("server", luckPerms.getServerName());
            var x = adapter.getUser(player).getNodes(NodeType.META).stream()
                    .filter(n -> n.getMetaKey().equals(META_KEY_PROVIDER_OVERRIDE) && n.getContexts().isSatisfiedBy(context))
                    .findFirst();
            return x.map(MetaNode::getMetaValue).orElse(null);
        } else {
            context.removeAll("world");
            context.add("world", world.getName());
            QueryOptions newQuery = query.toBuilder().context(context).build();
            cache = adapter.getUser(player).getCachedData().getMetaData(newQuery);
        }

        return cache.getMetaValue(META_KEY_PROVIDER_OVERRIDE);
    }

    private String getLPGameModeName(GameMode gameMode) {
        // https://github.com/LuckPerms/LuckPerms/blob/8fbd79139d34c97e13a486db554d12415f2a1fc2/bukkit/src/main/java/me/lucko/luckperms/bukkit/context/BukkitPlayerCalculator.java#L54
        return gameMode.name().toLowerCase();
    }

    private String getLPEnvironmentName(World.Environment environment) {
        // https://github.com/LuckPerms/LuckPerms/blob/8fbd79139d34c97e13a486db554d12415f2a1fc2/bukkit/src/main/java/me/lucko/luckperms/bukkit/context/BukkitPlayerCalculator.java#L58
        switch (environment) {
            case NORMAL -> { return "overworld"; }
            case NETHER -> { return "the_nether"; }
            case THE_END -> { return "the_end"; }
            default -> { return "custom"; }
        }
    }
}
