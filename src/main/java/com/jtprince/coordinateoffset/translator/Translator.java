package com.jtprince.coordinateoffset.translator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.jtprince.coordinateoffset.Offset;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class Translator {
    @NotNull
    public abstract Set<PacketType> getPacketTypes();

    public abstract @NotNull PacketContainer translate(@NotNull PacketContainer packet, @NotNull final Offset offset);
}
