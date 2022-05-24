package com.simibubi.create.content.curiosities.symmetry;

import java.util.function.Supplier;

import com.simibubi.create.content.curiosities.symmetry.mirror.SymmetryMirror;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ConfigureSymmetryWandPacket extends SimplePacketBase {

	protected InteractionHand hand;
	protected SymmetryMirror mirror;

	public ConfigureSymmetryWandPacket(InteractionHand hand, SymmetryMirror mirror) {
		this.hand = hand;
		this.mirror = mirror;
	}

	public ConfigureSymmetryWandPacket(FriendlyByteBuf buffer) {
		hand = buffer.readEnum(InteractionHand.class);
		mirror = SymmetryMirror.fromNBT(buffer.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeEnum(hand);
		buffer.writeNbt(mirror.writeToNbt());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof SymmetryWandItem) {
				SymmetryWandItem.configureSettings(stack, mirror);
			}
		});
		context.get().setPacketHandled(true);
	}

}
