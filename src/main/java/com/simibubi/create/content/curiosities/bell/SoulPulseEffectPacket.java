package com.simibubi.create.content.curiosities.bell;

import java.util.function.Supplier;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class SoulPulseEffectPacket extends SimplePacketBase {

	public BlockPos pos;
	public int distance;
	public boolean canOverlap;

	public SoulPulseEffectPacket(BlockPos pos, int distance, boolean overlaps) {
		this.pos = pos;
		this.distance = distance;
		this.canOverlap = overlaps;
	}

	public SoulPulseEffectPacket(FriendlyByteBuf buffer) {
		pos = buffer.readBlockPos();
		distance = buffer.readInt();
		canOverlap = buffer.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeInt(distance);
		buffer.writeBoolean(canOverlap);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			CreateClient.SOUL_PULSE_EFFECT_HANDLER.addPulse(new SoulPulseEffect(pos, distance, canOverlap));
		});
		context.get().setPacketHandled(true);
	}

}
