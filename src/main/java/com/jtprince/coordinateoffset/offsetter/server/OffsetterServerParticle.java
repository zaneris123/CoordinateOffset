package com.jtprince.coordinateoffset.offsetter.server;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;
import com.jtprince.coordinateoffset.offsetter.wrapper.ParticleVibrationData_EyeHeight;

public class OffsetterServerParticle extends PacketOffsetter<WrapperPlayServerParticle> {
    public OffsetterServerParticle() {
        super(WrapperPlayServerParticle.class, PacketType.Play.Server.PARTICLE);

        // TODO: Temporary fix until https://github.com/retrooper/packetevents/pull/661 merges in a PE release
        ParticleTypes.define("vibration", ParticleVibrationData_EyeHeight::read, (wrapper, data) -> {
            ParticleVibrationData_EyeHeight.write(wrapper, (ParticleVibrationData_EyeHeight)data);
        });
    }

    @Override
    public void offset(WrapperPlayServerParticle packet, Offset offset, User user) {
        packet.setPosition(apply(packet.getPosition(), offset));

        if (packet.getParticle().getData() instanceof ParticleVibrationData_EyeHeight vibrationData) {
            // startingPosition was only part of packet data up to 1.19.4. PE reports >1.19.4 with a zero vector.
            //  Make sure not to offset this zero vector, or it will leak offsets.
            if (!vibrationData.getStartingPosition().equals(Vector3i.zero())) {
                vibrationData.setStartingPosition(apply(vibrationData.getStartingPosition(), offset));
            }

            if (vibrationData.getBlockPosition().isPresent()) {
                vibrationData.setBlockPosition(apply(vibrationData.getBlockPosition().get(), offset));
            }
        }
    }
}
