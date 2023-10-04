package com.jtprince.coordinateoffset.translator;

import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.jtprince.coordinateoffset.Offset;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PacketContainerUtils {
    public static PacketContainer sendDouble2D(final PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getDoubles().size(), 2);
        packet.getDoubles().modify(0, x -> x == null ? null : x - offset.x());
        packet.getDoubles().modify(1, z -> z == null ? null : z - offset.z());
        return packet;
    }

    public static PacketContainer sendDouble3D(final PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getDoubles().size(), 3);
        packet.getDoubles().modify(0, x -> x == null ? null : x - offset.x());
        packet.getDoubles().modify(2, z -> z == null ? null : z - offset.z());
        return packet;
    }

    public static PacketContainer recvDouble3D(final PacketContainer packet, final Offset offset) {
        return sendDouble3D(packet, offset.negate());
    }

    public static PacketContainer sendInt3DTimes8(final PacketContainer packet, final Offset offset) {
        /*
         * From Protocol wiki, Sound Effect packet: "Effect X/Y/Z multiplied by 8 (fixed-point number with only 3 bits
         * dedicated to the fractional part)."
         */
        assertAtLeast(packet, packet.getIntegers().size(), 3);
        packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - (offset.x() << 3));
        packet.getIntegers().modify(2, curr_z -> curr_z == null ? null : curr_z - (offset.z() << 3));
        return packet;
    }

    public static PacketContainer sendChunkCoordinate(final PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getIntegers().size(), 2);
        packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - offset.chunkX());
        packet.getIntegers().modify(1, curr_z -> curr_z == null ? null : curr_z - offset.chunkZ());
        return packet;
    }

    public static PacketContainer sendBlockPosition(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getBlockPositionModifier().size(), 1);
        packet.getBlockPositionModifier().modify(0, offset::apply);
        return packet;
    }

    public static PacketContainer recvBlockPosition(PacketContainer packet, final Offset offset) {
        return sendBlockPosition(packet, offset.negate());
    }

    public static PacketContainer sendBlockPositionCollection(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getBlockPositionCollectionModifier().size(), 1);
        packet.getBlockPositionCollectionModifier().modify(0, col -> {
            if (col == null) return null;
            return col.stream().map(offset::apply).toList();
        });
        return packet;
    }

    public static PacketContainer sendSectionPosition(PacketContainer packet, final Offset offset) {
        // Section positions are encoded Block Positions - https://wiki.vg/Protocol#Update_Section_Blocks
        assertAtLeast(packet, packet.getSectionPositions().size(), 1);
        packet.getSectionPositions().modify(0, sp -> sp.subtract(new BlockPosition(offset.chunkX(), 0, offset.chunkZ())));
        return packet;
    }

    public static PacketContainer recvMovingBlockPosition(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getMovingBlockPositions().size(), 1);
        packet.getMovingBlockPositions().modify(0, pos -> {
            // NOTE: This is a recv function, these are inverse!
            pos.setBlockPosition(offset.unapply(pos.getBlockPosition()));
            pos.setPosVector(pos.getPosVector().add(new Vector(offset.x(), 0, offset.z())));
            return pos;
        });
        return packet;
    }

    @SuppressWarnings("rawtypes")
    public static PacketContainer sendPossiblyRelativePosition(PacketContainer packet, final Offset offset) {
        boolean isRelativeX = false;
        boolean isRelativeZ = false;
        Set<Enum> items = packet.getSets(Converters.passthrough(Enum.class)).read(0);
        for (Enum item : items) {
            switch (item.name()) {
                case "X":
                    isRelativeX = true;
                    break;
                case "Z":
                    isRelativeZ = true;
                    break;
            }
        }
        if (isRelativeX && isRelativeZ) return packet;
        assertAtLeast(packet, packet.getDoubles().size(), 3);
        if (!isRelativeX) {
            packet.getDoubles().modify(0, x -> x == null ? null : x - offset.x());
        }
        if (!isRelativeZ) {
            packet.getDoubles().modify(2, z -> z == null ? null : z - offset.z());
        }
        return packet;
    }

    public static PacketContainer sendItemStack(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getItemModifier().size(), 1);
        packet.getItemModifier().modify(0, itemStack -> {
            if (itemStack == null) return null;
            return transformItemStack(itemStack, offset);
        });
        return packet;
    }

    public static PacketContainer sendItemStackList(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getItemListModifier().size(), 1);
        packet.getItemListModifier().modify(0, stackList -> {
            if (stackList == null) return null;
            return stackList.stream().map(i -> transformItemStack(i, offset)).toList();
        });
        return packet;
    }

    private static @Nullable ItemStack transformItemStack(@Nullable ItemStack itemStack, final Offset offset) {
        if (itemStack == null) return null;
        itemStack = itemStack.clone();

        if (!itemStack.hasItemMeta()) return itemStack;
        if (itemStack.getItemMeta() instanceof org.bukkit.inventory.meta.CompassMeta compassMeta) {
            Location lodestoneLocation = compassMeta.getLodestone();
            if (lodestoneLocation != null) {
                compassMeta.setLodestone(offset.apply(lodestoneLocation));
                if (!itemStack.setItemMeta(compassMeta)) {
                    throw new RuntimeException("Failed to apply compass Meta to ItemStack!");
                }
            }
        }
        return itemStack;
    }

    public static PacketContainer sendDeathLocation(PacketContainer packet, final Offset offset) {
        assertAtLeast(packet, packet.getOptionalStructures().size(), 1);
        packet.getOptionalStructures().modify(0, os -> {
            if (os.isEmpty()) return os;
            assertAtLeast(packet, os.get().getBlockPositionModifier().size(), 1);
            os.get().getBlockPositionModifier().modify(0, offset::apply);
            return os;
        });
        return packet;
    }

    public static PacketContainer sendVibrationParticle(PacketContainer packet, final Offset offset) {
        sendDouble3D(packet, offset);

        assertAtLeast(packet, packet.getStructures().size(), 1);
        InternalStructure outer = packet.getStructures().read(0);
        if (outer.getStructures().size() < 1) return packet;
        InternalStructure inner = outer.getStructures().read(0);
        if (inner.getBlockPositionModifier().size() < 1) return packet;
        inner.getBlockPositionModifier().modify(0, offset::apply);
        return packet;
    }

    public static PacketContainer sendTileEntityNbt(PacketContainer packet, final Offset offset) {
        sendBlockPosition(packet, offset);

        assertAtLeast(packet, packet.getNbtModifier().size(), 1);
        packet.getNbtModifier().modify(0, nbtBase -> {
            if (nbtBase instanceof NbtCompound) {
                final NbtCompound nbt = (NbtCompound) (((NbtCompound) nbtBase).deepClone());
                if (nbt.containsKey("x") && nbt.containsKey("z")) {
                    nbt.put("x", nbt.getInteger("x") - offset.x());
                    nbt.put("z", nbt.getInteger("z") - offset.z());
                }
                return nbt;
            } else {
                return nbtBase;
            }
        });
        return packet;
    }

    static void assertAtLeast(final PacketContainer packet, int value, int expected) {
        if (value < expected) {
            throw new AssertionError("Packet size error: " + value + " < " + expected + " in " + packet.getType().name());
        }
    }
}
