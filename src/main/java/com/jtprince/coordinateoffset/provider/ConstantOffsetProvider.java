package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.CoordinateOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetProvider;
import com.jtprince.coordinateoffset.OffsetProviderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class ConstantOffsetProvider extends OffsetProvider {
    final Offset offset;

    private ConstantOffsetProvider(String name, Offset offset) {
        super(name);
        this.offset = offset;
    }

    @Override
    public @NotNull Offset getOffset(@NotNull OffsetProviderContext context) {
        return offset;
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

            int offsetX = providerConfig.getInt("offsetX");
            int offsetZ = providerConfig.getInt("offsetZ");

            if (offsetX > OffsetProvider.OFFSET_MAX) {
                throw new IllegalArgumentException("Provider \"" + name + "\": offsetX is too large! (Max 30M)");
            }
            if (offsetZ > OffsetProvider.OFFSET_MAX) {
                throw new IllegalArgumentException("Provider \"" + name + "\": offsetZ is too large! (Max 30M)");
            }

            return new ConstantOffsetProvider(name, new Offset(offsetX, offsetZ));
        }
    }
}
