package com.simibubi.create.content.contraptions.components.structureMovement.gantry;

import java.util.function.Supplier;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;

public class GantryContraptionUpdatePacket extends SimplePacketBase {

	int entityID;
	double coord;
	double motion;

	public GantryContraptionUpdatePacket(int entityID, double coord, double motion) {
		this.entityID = entityID;
		this.coord = coord;
		this.motion = motion;
	}

	public GantryContraptionUpdatePacket(FriendlyByteBuf buffer) {
		entityID = buffer.readInt();
		coord = buffer.readFloat();
		motion = buffer.readFloat();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeFloat((float) coord);
		buffer.writeFloat((float) motion);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get()
			.enqueueWork(
				() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> GantryContraptionEntity.handlePacket(this)));
		context.get()
			.setPacketHandled(true);
	}

}
