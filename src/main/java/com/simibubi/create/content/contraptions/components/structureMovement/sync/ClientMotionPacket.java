package com.simibubi.create.content.contraptions.components.structureMovement.sync;

import java.util.function.Supplier;

import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import io.github.fabricators_of_create.porting_lib.mixin.common.accessor.ServerGamePacketListenerImplAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

public class ClientMotionPacket extends SimplePacketBase {

	private Vec3 motion;
	private boolean onGround;
	private float limbSwing;

	public ClientMotionPacket(Vec3 motion, boolean onGround, float limbSwing) {
		this.motion = motion;
		this.onGround = onGround;
		this.limbSwing = limbSwing;
	}

	public ClientMotionPacket(FriendlyByteBuf buffer) {
		motion = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		onGround = buffer.readBoolean();
		limbSwing = buffer.readFloat();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeFloat((float) motion.x);
		buffer.writeFloat((float) motion.y);
		buffer.writeFloat((float) motion.z);
		buffer.writeBoolean(onGround);
		buffer.writeFloat(limbSwing);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get()
			.enqueueWork(() -> {
				ServerPlayer sender = context.get()
					.getSender();
				if (sender == null)
					return;
				sender.setDeltaMovement(motion);
				sender.setOnGround(onGround);
				if (onGround) {
					sender.causeFallDamage(sender.fallDistance, 1, DamageSource.FALL);
					sender.fallDistance = 0;
					((ServerGamePacketListenerImplAccessor)sender.connection).port_lib$setAboveGroundTickCount(0);
				}
				AllPackets.channel.sendToClientsTracking(new LimbSwingUpdatePacket(sender.getId(), sender.position(), limbSwing), sender);
			});
		context.get()
			.setPacketHandled(true);
	}

}
