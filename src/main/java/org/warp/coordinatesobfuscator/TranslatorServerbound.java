package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.jtprince.coordinateoffset.Offset;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class TranslatorServerbound {

	private static final String USE_ITEM = "BLOCK_PLACE";
	private static final String BLOCK_PLACE = "USE_ITEM";

	public static void incoming(@NotNull Logger logger, @NotNull final PacketContainer packet, @NotNull final Offset offset) {
		if (offset.equals(Offset.ZERO)) {
			return;
		}
		switch (packet.getType().name()) {
			case "POSITION":
			case "VEHICLE_MOVE":
			case "POSITION_LOOK":
				// PlayerManager.setLastPlayerLocation(player, player.getLocation()); // TODO
				recvDouble(logger, packet, offset);
				break;
			case "STRUCT":
			case "SET_JIGSAW":
			case "SET_COMMAND_BLOCK":
			case "UPDATE_SIGN":
			case "BLOCK_DIG":
				recvPosition(logger, packet, offset);
				break;
			case USE_ITEM:
				break;
			case BLOCK_PLACE:
				recvMovingPosition(logger, packet, offset);
				break;
			case "USE_ENTITY":
				break;
			default:
				break;
		}
	}

	private static void recvDouble(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getDoubles().size() > 2) {
			packet.getDoubles().modify(0, x -> x == null ? null : x + offset.x());
			packet.getDoubles().modify(2, z -> z == null ? null : z + offset.z());
		} else {
			int size = packet.getDoubles().size();
			logger.severe("Packet size error: " + size);
		}
	}


	private static void recvInt(Logger logger, final PacketContainer packet, final Offset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x + offset.x());
			packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z + offset.z());
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void recvPosition(Logger logger, final PacketContainer packet, final Offset offset) {
		if (packet.getBlockPositionModifier().size() > 0) {
			packet.getBlockPositionModifier().modify(0, offset::unapply);
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void recvMovingPosition(Logger logger, final PacketContainer packet, final Offset offset) {
		var mopb = packet.getMovingBlockPositions().read(0);
		if (mopb == null) {
			return;
		}
		mopb.setBlockPosition(mopb.getBlockPosition().add(new BlockPosition(offset.x(), 0, offset.z())));
		mopb.setPosVector(mopb.getPosVector().add(new Vector(offset.x(), 0, offset.z())));
		packet.getMovingBlockPositions().write(0, mopb);
	}
}
