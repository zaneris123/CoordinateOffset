package com.jtprince.coordinateoffset.translator.R1_19;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.translator.PacketContainerUtils;
import com.jtprince.coordinateoffset.translator.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Translator for Minecraft 1.19, 1.19.1, 1.19.2 (protocol 759-760)
 * <a href="https://wiki.vg/index.php?title=Protocol&oldid=17873">Wiki.vg</a>
 */
@SuppressWarnings({"DuplicatedCode", "deprecation", "RedundantSuppression"})
public class TranslatorServerboundR1_19 extends Translator.Serverbound {
    private final Map<PacketType, BiFunction<PacketContainer, Offset, PacketContainer>> translators = getTranslators();

    @Override
    public @NotNull Set<PacketType> getPacketTypes() {
        return translators.keySet();
    }

    @Override
    public @Nullable PacketContainer translate(@NotNull PacketEvent packetEvent, @NotNull Offset offset) {
        PacketContainer packet = packetEvent.getPacket();
        var translatorFunction = translators.get(packet.getType());
        if (translatorFunction != null) {
            return translatorFunction.apply(packet, offset);
        } else {
            return packet;
        }
    }

    private Map<PacketType, BiFunction<PacketContainer, Offset, PacketContainer>> getTranslators() {
        Map<PacketType, BiFunction<PacketContainer, Offset, PacketContainer>> map = new HashMap<>();

        map.put(PacketType.Play.Client.POSITION, PacketContainerUtils::recvDouble3D); // 0x14
        map.put(PacketType.Play.Client.POSITION_LOOK, PacketContainerUtils::recvDouble3D); // 0x15
        map.put(PacketType.Play.Client.VEHICLE_MOVE, PacketContainerUtils::recvDouble3D); // 0x18
        map.put(PacketType.Play.Client.BLOCK_DIG, PacketContainerUtils::recvBlockPosition); // 0x1D
        map.put(PacketType.Play.Client.SET_COMMAND_BLOCK, PacketContainerUtils::recvBlockPosition); // 0x29
        map.put(PacketType.Play.Client.SET_JIGSAW, PacketContainerUtils::recvBlockPosition); // 0x2C
        map.put(PacketType.Play.Client.STRUCT, PacketContainerUtils::recvBlockPosition); // 0x2D
        map.put(PacketType.Play.Client.UPDATE_SIGN, PacketContainerUtils::recvBlockPosition); // 0x2E
        map.put(PacketType.Play.Client.USE_ITEM, PacketContainerUtils::recvMovingBlockPosition); // 0x31

        return map;
    }
}
