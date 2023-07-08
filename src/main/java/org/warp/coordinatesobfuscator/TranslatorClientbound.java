package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.jtprince.coordinateoffset.Offset;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class TranslatorClientbound {

	private static final Class<?> NMS_BLOCK_POSITION_CLASS;
	private static final Method NMS_BLOCK_POSITION_ADD_CLASS;
	private static final EquivalentConverter<InternalStructure> INTERNAL_STRUCTURE_CONVERTER;

	static {
		NMS_BLOCK_POSITION_CLASS = MinecraftReflection.getBlockPositionClass();

		Method blockPositionAddMethod = null;
		try {
			blockPositionAddMethod = NMS_BLOCK_POSITION_CLASS.getDeclaredMethod("offset", int.class, int.class, int.class);
		} catch (NoSuchMethodException e) {
			try {
				blockPositionAddMethod = NMS_BLOCK_POSITION_CLASS.getDeclaredMethod("c", int.class, int.class, int.class);
			} catch (NoSuchMethodException e2) {
				e2.printStackTrace();
			}
		}
		NMS_BLOCK_POSITION_ADD_CLASS = blockPositionAddMethod;

		EquivalentConverter<InternalStructure> internalStructureEquivalentConverter = null;
		try {
			Field internalStructureConverter = InternalStructure.class.getDeclaredField("CONVERTER");
			internalStructureConverter.setAccessible(true);
			internalStructureEquivalentConverter = (EquivalentConverter<InternalStructure>) internalStructureConverter.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		INTERNAL_STRUCTURE_CONVERTER = internalStructureEquivalentConverter;
	}

	public static @Nullable PacketContainer outgoing(@NotNull Logger logger, @NotNull PacketContainer packet, @NotNull final Offset offset) {
		if (offset.equals(Offset.ZERO)) {
			return null;
		}

		// Some packets need a clone before we can manipulate them, or else we'll manipulate NMS-internal structures.
		boolean cloneNeeded = false;
		switch (packet.getType().name()) {
			case "TILE_ENTITY_DATA" -> {
				packet = cloneTileEntityData(packet);
				cloneNeeded = true;
			}
			case "MAP_CHUNK" -> {
				packet = cloneMapChunkEntitiesData(packet);
				cloneNeeded = true;
			}
			case "WORLD_PARTICLES", "LOGIN", "RESPAWN" -> {
				packet = packet.deepClone();
				cloneNeeded = true;
			}
		}

		switch (packet.getType().name()) {
			case "WINDOW_DATA":
				break;
			case "WINDOW_ITEMS":
				fixWindowItems(logger, packet, offset);
				break;
			case "SET_SLOT":
				fixWindowItem(logger, packet, offset);
				break;
			case "SPAWN_POSITION":
			case "WORLD_EVENT":
			case "UPDATE_SIGN":
			case "BLOCK_BREAK_ANIMATION":
			case "BLOCK_ACTION":
			case "BLOCK_CHANGE":
			case "OPEN_SIGN_EDITOR":
				sendBlockPosition(logger, packet, offset);
				break;
			case "LOGIN":
			case "RESPAWN":
				fixDeathLocation(logger, packet, offset);
				break;
			case "POSITION_LOOK":
			case "POSITION":
				boolean isRelativeX = false;
				boolean isRelativeZ = false;
				Set<Enum> items = packet.getSets(Converters.passthrough(Enum.class)).read(0);
				//noinspection rawtypes
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
				if (!isRelativeX || !isRelativeZ) {
					logger.fine("[out]Repositioning player");
					if (packet.getDoubles().size() > 2) {
						if (!isRelativeX) {
							packet.getDoubles().modify(0, x -> x == null ? null : x - offset.x());
						}
						if (!isRelativeZ) {
							packet.getDoubles().modify(2, z -> z == null ? null : z - offset.z());
						}
					} else {
						logger.severe("Packet size error");
					}
				}
				break;
			case "BED":
				//todo: remove
				//sendInt(packet, offset, 1);
				break;
			case "NAMED_ENTITY_SPAWN":
			case "SPAWN_ENTITY":
				Objects.requireNonNull(offset);
				sendDouble(logger, packet, offset);
				break;
			case "SPAWN_ENTITY_WEATHER":
			case "SPAWN_ENTITY_EXPERIENCE_ORB":
			case "ENTITY_TELEPORT":
				sendDouble(logger, packet, offset);
				break;
			case "WORLD_PARTICLES":
				sendDouble(logger, packet, offset);
				fixVibrationParticle(logger, packet, offset);
				break;
			case "UNLOAD_CHUNK":
				sendChunk(logger, packet, offset, false, false);
				break;
			case "LIGHT_UPDATE":
				sendChunk(logger, packet, offset, false, true);
				break;
			case "MAP_CHUNK":
				sendChunk(logger, packet, offset, true, false);
				break;
			case "MULTI_BLOCK_CHANGE":
				sendChunkUpdate(logger, packet, offset);
				break;
			case "VIEW_CENTRE":
				sendIntChunk(logger, packet, offset);
				break;
			case "MAP_CHUNK_BULK":
				sendChunkBulk(logger, packet, offset);
				break;
			case "EXPLOSION":
				sendExplosion(logger, packet, offset);
				break;
			case "NAMED_SOUND_EFFECT":
				sendInt8(logger, packet, offset);
				break;
			case "ENTITY_METADATA":
				if (packet.getDataValueCollectionModifier().size() > 0) {
					packet.getDataValueCollectionModifier().modify(0, wrappedDataValues -> {
						if (wrappedDataValues == null) return null;
						ArrayList<WrappedDataValue> result = new ArrayList<WrappedDataValue>(wrappedDataValues.size());
						for (WrappedDataValue wrappedWatchableObject : wrappedDataValues) {
							if (wrappedWatchableObject == null) {
								result.add(null);
								continue;
							}

							Object oldValue = wrappedWatchableObject.getValue();
							if (oldValue instanceof Optional) {
								//noinspection rawtypes
								Optional opt = (Optional) oldValue;
								if (opt.isPresent()) {
									Object val = opt.get();
									if (NMS_BLOCK_POSITION_CLASS.isInstance(val)) {
										wrappedWatchableObject.setValue(Optional.of(offsetPositionMc(logger, offset, val)));
									}
								}
							} else if (oldValue instanceof BlockPosition blockPosition) {
								wrappedWatchableObject.setValue(offsetPosition(logger, offset, blockPosition));
							}
							result.add(wrappedWatchableObject);
						}
						return result;
					});
				}
				break;
			case "TILE_ENTITY_DATA":
				sendTileEntityData(logger, packet, offset);
				break;
			case "PLAYER_INFO":
				break;
			default:
				logger.fine(packet.getType().name());
				break;
		}

		if (cloneNeeded) return packet; else return null;
	}

	private static void fixDeathLocation(Logger logger, PacketContainer packet, Offset offset) {
		if (packet.getOptionalStructures().size() > 0) {
			packet.getOptionalStructures().modify(0, p -> {
				p.ifPresent(internalStructure -> internalStructure.getBlockPositionModifier().modify(0, q -> offsetPosition(logger, offset, q)));
				return p;
			});
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void fixWindowItems(Logger logger, PacketContainer packet, Offset offset) {
		packet.getItemListModifier().modify(0, itemStacks -> {
			if (itemStacks == null) return null;
			List<ItemStack> newItems = new ArrayList<>(itemStacks.size());
			for (ItemStack itemStack : itemStacks) {
				if (itemStack == null) {
					newItems.add(null);
					continue;
				}
				newItems.add(transformItemStack(logger, itemStack, offset));
			}
			return newItems;
		});
	}

	private static void fixWindowItem(Logger logger, PacketContainer packet, Offset offset) {
		packet.getItemModifier().modify(0, itemStack -> {
			if (itemStack == null) {
				return null;
			}
			return transformItemStack(logger, itemStack, offset);
		});
	}

	private static ItemStack transformItemStack(Logger logger, ItemStack itemStack, Offset offset) {
		itemStack = itemStack.clone();

		if (itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();

			if (itemMeta instanceof org.bukkit.inventory.meta.CompassMeta) {
				org.bukkit.inventory.meta.CompassMeta compassMeta = (org.bukkit.inventory.meta.CompassMeta) itemMeta;
				Location lodestoneLocation = compassMeta.getLodestone();
				if (lodestoneLocation != null) {
					compassMeta.setLodestone(lodestoneLocation.subtract(offset.x(), 0, offset.z()));
					if (!itemStack.setItemMeta(compassMeta)) {
						logger.severe("Can't apply meta");
					}
				}
			}
		}
		return itemStack;
	}


	private static void sendChunk(Logger logger, final PacketContainer packet, final Offset offset, boolean includesEntities, boolean includeLight) {
		StructureModifier<Integer> integers = packet.getIntegers();
		integers.modify(0, curr_x -> {
			if (curr_x != null) {
				return curr_x - offset.chunkX();
			} else {
				return null;
			}
		});
		integers.modify(1, curr_z -> {
			if (curr_z != null) {
				return curr_z - offset.chunkZ();
			} else {
				return null;
			}
		});
		if (includeLight) {
			StructureModifier<List<byte[]>> byteArrays = packet.getStructures().read(0).getLists(Converters.passthrough(byte[].class));
			for (int i = 0; i < 2; i++) {
				byteArrays.modify(i, list -> {
					if (list == null) return null;
					ArrayList<byte[]> newList = new ArrayList<>(list);
					newList.replaceAll(bytes -> Arrays.copyOf(bytes, bytes.length));
					return newList;
				});
			}
		}
		if (includesEntities) {
			packet.getStructures().read(0).getLists(INTERNAL_STRUCTURE_CONVERTER).modify(0, entities -> {
				if (entities == null) return null;
				for (InternalStructure entity : entities) {
					// ((blockX & 15) << 4) | (blockZ & 15)
					entity.getIntegers().modify(0, packedXZ -> {
						int x = (packedXZ >> 4) - offset.x();
						int y = (packedXZ & 15) - offset.z();
						return ((x & 15) << 4) | (y & 15);
					});
				}
				return entities;
			});
		}
	}

	private static void sendChunkBulk(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getIntegerArrays().size() > 1) {
			final int[] x = packet.getIntegerArrays().read(0).clone();
			final int[] z = packet.getIntegerArrays().read(1).clone();

			for (int i = 0; i < x.length; i++) {

				x[i] = x[i] - offset.chunkX();
				z[i] = z[i] - offset.chunkZ();
			}

			packet.getIntegerArrays().write(0, x);
			packet.getIntegerArrays().write(1, z);
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendChunkUpdate(Logger logger, final PacketContainer packet, final Offset offset) {
		BlockPosition sp = packet.getSectionPositions().read(0);
		if (sp == null) {
			return;
		}
		packet.getSectionPositions().write(0, sp.subtract(new BlockPosition(offset.chunkX(), 0, offset.chunkZ())));
	}


	private static void sendDouble(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getDoubles().size() > 2) {
			packet.getDoubles().modify(0, x -> x == null ? null : x - offset.x());
			packet.getDoubles().modify(2, z -> z == null ? null : z - offset.z());
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendExplosion(Logger logger, final PacketContainer packet, final Offset offset) {
		sendDouble(logger, packet, offset);

		packet.getBlockPositionCollectionModifier().modify(0, lst -> {
			if (lst == null) return null;
			ArrayList<BlockPosition> newLst = new ArrayList<BlockPosition>(lst.size());
			for (BlockPosition blockPosition : lst) {
				newLst.add(blockPosition.subtract(new BlockPosition(offset.x(), 0, offset.z())));
			}
			return newLst;
		});
	}


	private static void sendFixedPointNumber(Logger logger, final PacketContainer packet, final Offset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x - (offset.x() << 5));
			packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z - (offset.z() << 5));
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendFloat(Logger logger, final PacketContainer packet, final Offset offset, final int index) {
		if (packet.getFloat().size() > 2) {
			packet.getFloat().modify(index, curr_x -> curr_x == null ? null : (float) (curr_x - offset.x()));
			packet.getFloat().modify(index + 2, curr_z -> curr_z == null ? null : (float) (curr_z - offset.z()));
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendInt(Logger logger, final PacketContainer packet, final Offset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x - offset.x());
			packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z - offset.z());
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendIntChunk(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getIntegers().size() > 1) {
			packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - offset.chunkX());
			packet.getIntegers().modify(1, curr_z -> curr_z == null ? null : curr_z - offset.chunkZ());
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendBlockPosition(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getBlockPositionModifier().size() > 0) {
			packet.getBlockPositionModifier().modify(0, pos -> offsetPosition(logger, offset, pos));
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void fixVibrationParticle(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getStructures().size() > 0) {
			InternalStructure outerStructure = packet.getStructures().read(0);
			if (outerStructure.getStructures().size() > 0) {
				InternalStructure innerStructure = outerStructure.getStructures().read(0);
				if (innerStructure.getBlockPositionModifier().size() > 0) {
					innerStructure.getBlockPositionModifier().modify(0, p -> offsetPosition(logger, offset, p));
				}
			}
		} else {
			logger.severe("Packet size error");
		}
	}

	private static BlockPosition offsetPosition(Logger logger, Offset offset, BlockPosition pos) {
		if (pos == null) return null;
		return pos.subtract(new BlockPosition(offset.x(), 0, offset.z()));
	}

	private static Object offsetPositionMc(Logger logger, Offset offset, Object pos) {
		if (pos == null) return null;
		try {
			return NMS_BLOCK_POSITION_ADD_CLASS.invoke(pos, -offset.x(), -0, -offset.z());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}


	private static void sendInt8(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - (offset.x() << 3));
			packet.getIntegers().modify(2, curr_z -> curr_z == null ? null : curr_z - (offset.z() << 3));
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendTileEntityData(Logger logger, final PacketContainer packet, final Offset offset) {
		sendBlockPosition(logger, packet, offset);

		packet.getNbtModifier().modify(0, nbtBase -> {
			if (nbtBase == null) return null;
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
	}

	private static PacketContainer cloneTileEntityData(PacketContainer packet) {
		packet = packet.shallowClone();
		int i = 0;
		for (final NbtBase<?> obj : packet.getNbtModifier().getValues()) {
			if (obj == null) continue;
			packet.getNbtModifier().write(i, obj.deepClone());
			i++;
		}

		return packet;
	}

	private static PacketContainer cloneMapChunkEntitiesData(PacketContainer packet) {
		packet = packet.shallowClone();
		int i = 0;
		for (final List<NbtBase<?>> obj : packet.getListNbtModifier().getValues()) {
			ArrayList<NbtBase<?>> newList = new ArrayList<NbtBase<?>>(obj.size());
			for (NbtBase<?> nbtBase : obj) {
				newList.add(nbtBase.deepClone());
			}
			packet.getListNbtModifier().write(i, newList);
			i++;
		}

		return packet;
	}
}
