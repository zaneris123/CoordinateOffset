package com.jtprince.coordinateoffset.translator.R1_20_2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.translator.EntityMetadataUtils;
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
public class TranslatorClientboundR1_20_2 extends Translator.Clientbound {
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
            /*
             * Deep clone: Outbound packets can contain nested objects that are reused when the packet gets sent to
             * successive players. That means that just editing values in the packet in-place would propagate the
             * edited values to all players, and cause lots of glitches (those glitches are only apparent with multiple
             * online players). It would be optimal to only deep-clone the packets that need this, but risky, since it
             * is not always obvious when a packet type will start reusing nested objects.
             */
            return translatorFunction.apply(packet.deepClone(), offset);
        } else {
            return packet;
        }
    }

    private Map<PacketType, BiFunction<PacketContainer, Offset, PacketContainer>> getTranslators() {
        Map<PacketType, BiFunction<PacketContainer, Offset, PacketContainer>> map = new HashMap<>();

        map.put(PacketType.Play.Server.SPAWN_ENTITY, PacketContainerUtils::sendDouble3D); // 0x01
        map.put(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB, PacketContainerUtils::sendDouble3D); // 0x02
        map.put(PacketType.Play.Server.BLOCK_BREAK_ANIMATION, PacketContainerUtils::sendBlockPosition); // 0x06
        map.put(PacketType.Play.Server.TILE_ENTITY_DATA, PacketContainerUtils::sendTileEntityNbt); // 0x07
        map.put(PacketType.Play.Server.BLOCK_ACTION, PacketContainerUtils::sendBlockPosition); // 0x08
        map.put(PacketType.Play.Server.BLOCK_CHANGE, PacketContainerUtils::sendBlockPosition); // 0x09
        map.put(PacketType.Play.Server.WINDOW_ITEMS, PacketContainerUtils::sendItemStackList); // 0x13
        map.put(PacketType.Play.Server.SET_SLOT, PacketContainerUtils::sendItemStack); // 0x15
        map.put(PacketType.Play.Server.EXPLOSION, (pkt, offset) ->  // 0x1E
                PacketContainerUtils.sendBlockPositionCollection(PacketContainerUtils.sendDouble3D(pkt, offset), offset));
        map.put(PacketType.Play.Server.UNLOAD_CHUNK, PacketContainerUtils::sendChunkCoordIntPairs); // 0x1F
        map.put(PacketType.Play.Server.MAP_CHUNK, PacketContainerUtils::sendChunkCoordinate); // 0x25
        map.put(PacketType.Play.Server.WORLD_EVENT, PacketContainerUtils::sendBlockPosition); // 0x26
        map.put(PacketType.Play.Server.WORLD_PARTICLES, PacketContainerUtils::sendParticle); // 0x27
        map.put(PacketType.Play.Server.LIGHT_UPDATE, PacketContainerUtils::sendChunkCoordinate); // 0x28
        map.put(PacketType.Play.Server.LOGIN, (pkt, offset) -> // 0x29
                PacketContainerUtils.sendDeathLocation1_20_2(pkt, offset, 1)
        );
        map.put(PacketType.Play.Server.OPEN_SIGN_EDITOR, PacketContainerUtils::sendBlockPosition); // 0x32
        map.put(PacketType.Play.Server.POSITION, PacketContainerUtils::sendPossiblyRelativePosition); // 0x3E
        map.put(PacketType.Play.Server.RESPAWN, (pkt, offset) -> // 0x43
                PacketContainerUtils.sendDeathLocation1_20_2(pkt, offset, 0)
        );
        map.put(PacketType.Play.Server.MULTI_BLOCK_CHANGE, PacketContainerUtils::sendSectionPosition); // 0x45
        map.put(PacketType.Play.Server.VIEW_CENTRE, PacketContainerUtils::sendChunkCoordinate); // 0x50
        map.put(PacketType.Play.Server.SPAWN_POSITION, PacketContainerUtils::sendBlockPosition); // 0x52
        map.put(PacketType.Play.Server.ENTITY_METADATA, EntityMetadataUtils::sendEntityMetadata); // 0x54
        map.put(PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketContainerUtils::sendInt3DTimes8); // 0x64
        map.put(PacketType.Play.Server.ENTITY_TELEPORT, PacketContainerUtils::sendDouble3D); // 0x6B

        return map;
    }
}
