package com.jtprince.coordinateoffset.provider.util;

import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.configuration.ConfigurationSection;

public class ResetConfig {
    private boolean resetOnRespawn;
    private boolean resetOnWorldChange;
    private boolean resetOnDistantTeleport;

    public boolean resetOn(OffsetProviderContext.ProvideReason reason) {
        switch (reason) {
            case RESPAWN -> { return resetOnRespawn; }
            case WORLD_CHANGE -> { return resetOnWorldChange; }
            case DISTANT_TELEPORT -> { return resetOnDistantTeleport; }
            default -> { return false; }
        }
    }

    private ResetConfig() {}

    public static ResetConfig fromConfigSection(ConfigurationSection providerConfig) {
        ResetConfig p = new ResetConfig();
        p.resetOnRespawn = providerConfig.getBoolean("resetOnRespawn");
        p.resetOnWorldChange = providerConfig.getBoolean("resetOnWorldChange");
        p.resetOnDistantTeleport = providerConfig.getBoolean("resetOnDistantTeleport");
        return p;
    }
}
