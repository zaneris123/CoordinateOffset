package com.jtprince.coordinateoffset.translator;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.jtprince.coordinateoffset.Offset;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static com.jtprince.coordinateoffset.translator.PacketContainerUtils.assertAtLeast;

public class EntityMetadataUtils {
    private static final Class<?> NMS_BLOCK_POSITION_CLASS;
    private static final Method NMS_BLOCK_POSITION_ADD_METHOD;
    static {
        NMS_BLOCK_POSITION_CLASS = MinecraftReflection.getBlockPositionClass();
        Method blockPositionAddMethod = null;
        try {
            blockPositionAddMethod = NMS_BLOCK_POSITION_CLASS.getDeclaredMethod("offset", int.class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            try {
                blockPositionAddMethod = NMS_BLOCK_POSITION_CLASS.getDeclaredMethod("c", int.class, int.class, int.class);
            } catch (NoSuchMethodException e2) {
                //noinspection CallToPrintStackTrace
                e2.printStackTrace();
            }
        }
        NMS_BLOCK_POSITION_ADD_METHOD = blockPositionAddMethod;
    }

    public static PacketContainer sendEntityMetadata1_19_3(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getDataValueCollectionModifier().size(), 1);
        packet.getDataValueCollectionModifier().modify(0, wrappedDataValues -> {
            if (wrappedDataValues == null) return null;
            return wrappedDataValues.stream().map(wrap -> {
                if (wrap == null) return null;
                Object data = wrap.getValue();
                if (data instanceof Optional<?> optional) {
                    wrap.setValue(optional.map(object -> applyOffsetToEntityMeta(object, offset)));
                } else {
                    wrap.setValue(applyOffsetToEntityMeta(data, offset));
                }
                return wrap;
            }).toList();
        });
        return packet;
    }

    public static PacketContainer sendEntityMetadata1_18(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getWatchableCollectionModifier().size(), 1);
        packet.getWatchableCollectionModifier().modify(0, wrappedWatchableObject -> {
            if (wrappedWatchableObject == null) return null;
            return wrappedWatchableObject.stream().map(wrap -> {
                if (wrap == null) return null;
                Object data = wrap.getValue();
                if (data instanceof Optional<?> optional) {
                    wrap.setValue(optional.map(object -> applyOffsetToEntityMeta(object, offset)));
                } else {
                    wrap.setValue(applyOffsetToEntityMeta(data, offset));
                }
                return wrap;
            }).toList();
        });
        return packet;
    }

    private static Object applyOffsetToEntityMeta(Object object, Offset offset) {
        if (NMS_BLOCK_POSITION_CLASS.isInstance(object)) {
            return offsetPositionMc(object, offset);
        }
        if (object instanceof BlockPosition blockPosition) {
            return offset.apply(blockPosition);
        }
        // throw new RuntimeException("Unable to apply offset to Entity metadata! " + object);
        return object;
    }

    // TODO: Try using ProtocolLib helpers to clean this up.
    private static Object offsetPositionMc(Object pos, Offset offset) {
        if (pos == null) return null;
        try {
            return NMS_BLOCK_POSITION_ADD_METHOD.invoke(pos, -offset.x(), 0, -offset.z());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
