package com.jtprince.coordinateoffset.translator.R1_20_2;

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
 * Translator for Minecraft 1.20.2 (Protocol 764)
 * <a href="https://wiki.vg/Protocol">Wiki.vg</a>
 */
@SuppressWarnings("DuplicatedCode")
public class TranslatorServerboundR1_20_2 extends Translator.Serverbound {
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

        map.put(PacketType.Play.Client.POSITION, PacketContainerUtils::recvDouble3D); // 0x16
        map.put(PacketType.Play.Client.POSITION_LOOK, PacketContainerUtils::recvDouble3D); // 0x17
        map.put(PacketType.Play.Client.VEHICLE_MOVE, PacketContainerUtils::recvDouble3D); // 0x1A
        map.put(PacketType.Play.Client.BLOCK_DIG, PacketContainerUtils::recvBlockPosition); // 0x20
        map.put(PacketType.Play.Client.SET_COMMAND_BLOCK, PacketContainerUtils::recvBlockPosition); // 0x2C
        map.put(PacketType.Play.Client.SET_JIGSAW, PacketContainerUtils::recvBlockPosition); // 0x2F
        map.put(PacketType.Play.Client.STRUCT, PacketContainerUtils::recvBlockPosition); // 0x30
        map.put(PacketType.Play.Client.UPDATE_SIGN, PacketContainerUtils::recvBlockPosition); // 0x31
        map.put(PacketType.Play.Client.USE_ITEM, PacketContainerUtils::recvMovingBlockPosition); // 0x34

        return map;
    }
}
