package com.jtprince.coordinateoffset.offsetter.client;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OffsetterClientClickWindow extends PacketOffsetter<WrapperPlayClientClickWindow> {
    public OffsetterClientClickWindow() {
        super(WrapperPlayClientClickWindow.class, PacketType.Play.Client.CLICK_WINDOW);
    }

    @Override
    public void offset(WrapperPlayClientClickWindow packet, Offset offset, User user) {
        if (packet.getSlots().isPresent()) {
            Map<Integer, ItemStack> clientItems = packet.getSlots().get();
            Map<Integer, ItemStack> serverItems = clientItems.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> unapplyItemStack(v.getValue(), offset)));
            packet.setSlots(Optional.of(serverItems));
        }
        packet.setCarriedItemStack(unapplyItemStack(packet.getCarriedItemStack(), offset));
    }
}
