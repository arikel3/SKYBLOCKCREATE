package com.simibubi.create.content.curiosities.zapper;

import java.util.function.Supplier;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public abstract class ConfigureZapperPacket extends SimplePacketBase {

	protected InteractionHand hand;
	protected PlacementPatterns pattern;

	public ConfigureZapperPacket(InteractionHand hand, PlacementPatterns pattern) {
		this.hand = hand;
		this.pattern = pattern;
	}

	public ConfigureZapperPacket(FriendlyByteBuf buffer) {
		hand = buffer.readEnum(InteractionHand.class);
		pattern = buffer.readEnum(PlacementPatterns.class);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeEnum(hand);
		buffer.writeEnum(pattern);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof ZapperItem) {
				configureZapper(stack);
			}
		});
		context.get().setPacketHandled(true);
	}

	public abstract void configureZapper(ItemStack stack);

}
