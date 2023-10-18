package com.jtprince.coordinateoffset.translator.R1_17;

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
 * Translator for Minecraft 1.17, 1.17.1 (protocol 755-756)
 * <a href="https://wiki.vg/index.php?title=Protocol&oldid=17159">Wiki.vg</a>
 */
@SuppressWarnings({"DuplicatedCode", "deprecation", "RedundantSuppression"})
public class TranslatorServerboundR1_17 extends Translator.Serverbound {
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

        map.put(PacketType.Play.Client.POSITION, PacketContainerUtils::recvDouble3D); // 0x11
        map.put(PacketType.Play.Client.POSITION_LOOK, PacketContainerUtils::recvDouble3D); // 0x12
        map.put(PacketType.Play.Client.VEHICLE_MOVE, PacketContainerUtils::recvDouble3D); // 0x15
        map.put(PacketType.Play.Client.BLOCK_DIG, PacketContainerUtils::recvBlockPosition); // 0x1A
        map.put(PacketType.Play.Client.SET_COMMAND_BLOCK, PacketContainerUtils::recvBlockPosition); // 0x26
        map.put(PacketType.Play.Client.SET_JIGSAW, PacketContainerUtils::recvBlockPosition); // 0x29
        map.put(PacketType.Play.Client.STRUCT, PacketContainerUtils::recvBlockPosition); // 0x2A
        map.put(PacketType.Play.Client.UPDATE_SIGN, PacketContainerUtils::recvBlockPosition); // 0x2B
        map.put(PacketType.Play.Client.USE_ITEM, PacketContainerUtils::recvMovingBlockPosition); // 0x2E

        return map;
    }
}
