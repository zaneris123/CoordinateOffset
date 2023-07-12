package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConstantOffsetProvider extends OverworldOffsetProvider {
    final Offset overworldOffset;

    public ConstantOffsetProvider(String name, Offset overworldOffset) {
        super(name);
        this.overworldOffset = overworldOffset;
    }

    @Override
    public @NotNull Offset getOverworldOffset(@NotNull Player player) {
        return overworldOffset;
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<ConstantOffsetProvider> {
        @Override
        public @NotNull ConstantOffsetProvider createProvider(String name, CoordinateOffset plugin, ConfigurationSection providerConfig) throws IllegalArgumentException {
            if (!providerConfig.isInt("offsetX")) {
                throw new IllegalArgumentException("Missing field offsetX for ConstantOffsetProvider.");
            }
            if (!providerConfig.isInt("offsetZ")) {
                throw new IllegalArgumentException("Missing field offsetZ for ConstantOffsetProvider.");
            }

            return new ConstantOffsetProvider(name, new Offset(providerConfig.getInt("offsetX"), providerConfig.getInt("offsetZ")));
        }
    }
}
