package com.simibubi.create.content.logistics.item;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerBindPacket extends LinkedControllerPacketBase {

	private int button;
	private BlockPos linkLocation;

	public LinkedControllerBindPacket(int button, BlockPos linkLocation) {
		super((BlockPos) null);
		this.button = button;
		this.linkLocation = linkLocation;
	}

	public LinkedControllerBindPacket(FriendlyByteBuf buffer) {
		super(buffer);
		this.button = buffer.readVarInt();
		this.linkLocation = buffer.readBlockPos();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		super.write(buffer);
		buffer.writeVarInt(button);
		buffer.writeBlockPos(linkLocation);
	}

	@Override
	protected void handleItem(ServerPlayer player, ItemStack heldItem) {
		if (player.isSpectator())
			return;

		ItemStackHandler frequencyItems = LinkedControllerItem.getFrequencyItems(heldItem);
		LinkBehaviour linkBehaviour = TileEntityBehaviour.get(player.level, linkLocation, LinkBehaviour.TYPE);
		if (linkBehaviour == null)
			return;

		Pair<Frequency, Frequency> pair = linkBehaviour.getNetworkKey();
		frequencyItems.setStackInSlot(button * 2, pair.getKey()
			.getStack()
			.copy());
		frequencyItems.setStackInSlot(button * 2 + 1, pair.getValue()
			.getStack()
			.copy());

		heldItem.getTag().put("Items", frequencyItems.serializeNBT());
	}

	@Override
	protected void handleLectern(ServerPlayer player, LecternControllerTileEntity lectern) { }

}
