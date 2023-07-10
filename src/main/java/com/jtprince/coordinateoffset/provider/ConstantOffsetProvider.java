package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConstantOffsetProvider extends OverworldOffsetProvider {
    final Offset overworldOffset;

    public ConstantOffsetProvider(Offset overworldOffset) {
        this.overworldOffset = overworldOffset;
    }

    @Override
    public @NotNull Offset getOverworldOffset(@NotNull Player player) {
        return overworldOffset;
    }

    public static class ConfigFactory implements OffsetProvider.ConfigurationFactory<ConstantOffsetProvider> {
        @Override
        public @NotNull ConstantOffsetProvider createProvider(ConfigurationSection configSection) throws IllegalArgumentException {
            if (!configSection.isInt("offsetX")) {
                throw new IllegalArgumentException("Missing field offsetX for ConstantOffsetProvider.");
            }
            if (!configSection.isInt("offsetZ")) {
                throw new IllegalArgumentException("Missing field offsetZ for ConstantOffsetProvider.");
            }

            ConstantOffsetProvider p = new ConstantOffsetProvider(new Offset(configSection.getInt("offsetX"), configSection.getInt("offsetZ")));
            return p;
        }
    }
}
