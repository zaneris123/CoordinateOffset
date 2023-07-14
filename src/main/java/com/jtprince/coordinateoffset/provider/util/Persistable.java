package com.jtprince.coordinateoffset.provider.util;

import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.configuration.ConfigurationSection;

public class Persistable {
    private boolean persistAcrossRespawns;
    private boolean persistAcrossWorldChanges;
    private boolean persistAcrossDistantTeleports;

    public boolean canPersist(OffsetProviderContext.ProvideReason reason) {
        switch (reason) {
            case RESPAWN -> { return persistAcrossRespawns; }
            case WORLD_CHANGE -> { return persistAcrossWorldChanges; }
            case DISTANT_TELEPORT -> { return persistAcrossDistantTeleports; }
            default -> { return false; }
        }
    }

    private Persistable() {}

    public static Persistable fromConfigSection(ConfigurationSection providerConfig) {
        Persistable p = new Persistable();
        p.persistAcrossRespawns = providerConfig.getBoolean("persistAcrossRespawns");
        p.persistAcrossWorldChanges = providerConfig.getBoolean("persistAcrossWorldChanges");
        p.persistAcrossDistantTeleports = providerConfig.getBoolean("persistAcrossDistantTeleports");
        return p;
    }
}
