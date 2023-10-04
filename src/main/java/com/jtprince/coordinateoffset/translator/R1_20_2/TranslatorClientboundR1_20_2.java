package com.jtprince.coordinateoffset.translator.R1_20_2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.translator.Translator;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TranslatorClientboundR1_20_2 extends Translator {
    @Override
    public @NotNull Set<PacketType> getPacketTypes() {
        throw new NotImplementedException("1.20.2 TBD");
    }

    @Override
    public @NotNull PacketContainer translate(@NotNull PacketContainer packet, @NotNull Offset offset) {
        throw new NotImplementedException("1.20.2 TBD");
    }
}
