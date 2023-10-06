package com.jtprince.coordinateoffset.translator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.translator.R1_19_4.TranslatorClientboundR1_19_4;
import com.jtprince.coordinateoffset.translator.R1_19_4.TranslatorServerboundR1_19_4;
import com.jtprince.coordinateoffset.translator.R1_20_2.TranslatorClientboundR1_20_2;
import com.jtprince.coordinateoffset.translator.R1_20_2.TranslatorServerboundR1_20_2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A Translator provides packet offsetting via ProtocolLib for a specific range of supported Minecraft versions.
 * Updating the plugin to a new Minecraft version should ideally only require writing a new Translator.
 */
public abstract class Translator {

    /**
     * A container for Translators that support a specific range of supported Minecraft versions.
     * @param minVersion The earliest ProtocolLib Minecraft version that these Translators support.
     * @param maxStatedVersion Highest Minecraft version this Translator works with, or null if the minimum version is
     *                         equal to the maximum version. (This should ONLY be used for informational purposes)
     * @param clientbound A Translator for clientbound ("Server") packets.
     * @param serverbound A Translator for serverbound ("Client") packets.
     */
    public record Version(
            MinecraftVersion minVersion,
            @Nullable String maxStatedVersion,
            Class<? extends Translator> clientbound,
            Class<? extends Translator> serverbound
    ) {}

    /*
     * On ProtocolLib update, validate that CoordinateOffset behaves correctly and update this to the newest ProtocolLib
     * Minecraft Version when it does. The plugin may be used on a version past this and work in many circumstances, but
     * it will print a warning that the protocol may have changed.
     */
    public static final MinecraftVersion LATEST_SUPPORTED = MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE;
    /*
     * Add new translator versions here, most recently released Minecraft versions first.
     * If a translator works for multiple Minecraft versions, set its upper bound as the second argument.
     */
    public static final List<Version> VERSIONS = List.of(
            new Translator.Version(MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE, null,
                    TranslatorClientboundR1_20_2.class, TranslatorServerboundR1_20_2.class),
            new Translator.Version(MinecraftVersion.FEATURE_PREVIEW_2, "1.20.1",
                    TranslatorClientboundR1_19_4.class, TranslatorServerboundR1_19_4.class)
    );

    @NotNull
    public abstract Set<PacketType> getPacketTypes();

    public abstract @NotNull PacketContainer translate(@NotNull PacketContainer packet, @NotNull final Offset offset);
}
