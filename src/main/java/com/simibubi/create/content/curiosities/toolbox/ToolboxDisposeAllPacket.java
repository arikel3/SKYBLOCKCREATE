package com.simibubi.create.content.curiosities.toolbox;

import java.util.function.Supplier;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.EntityHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ToolboxDisposeAllPacket extends SimplePacketBase {

	private BlockPos toolboxPos;

	public ToolboxDisposeAllPacket(BlockPos toolboxPos) {
		this.toolboxPos = toolboxPos;
	}

	public ToolboxDisposeAllPacket(FriendlyByteBuf buffer) {
		toolboxPos = buffer.readBlockPos();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(toolboxPos);
	}

	@Override
	public void handle(Supplier<Context> context) {
		Context ctx = context.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			Level world = player.level;
			BlockEntity blockEntity = world.getBlockEntity(toolboxPos);

			double maxRange = ToolboxHandler.getMaxRange(player);
			if (player.distanceToSqr(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
				* maxRange)
				return;
			if (!(blockEntity instanceof ToolboxTileEntity))
				return;
			ToolboxTileEntity toolbox = (ToolboxTileEntity) blockEntity;

			CompoundTag compound = EntityHelper.getExtraCustomData(player)
				.getCompound("CreateToolboxData");
			MutableBoolean sendData = new MutableBoolean(false);

			toolbox.inventory.inLimitedMode(inventory -> {
				for (int i = 0; i < 36; i++) {
					String key = String.valueOf(i);
					if (compound.contains(key) && NbtUtils.readBlockPos(compound.getCompound(key)
						.getCompound("Pos"))
						.equals(toolboxPos)) {
						ToolboxHandler.unequip(player, i, true);
						sendData.setTrue();
					}

					ItemStack itemStack = player.getInventory().getItem(i);
					try (Transaction t = TransferUtil.getTransaction()) {
						long inserted = toolbox.inventory.insert(ItemVariant.of(itemStack), itemStack.getCount(), t);
						ItemStack remainder = ItemHandlerHelper.copyStackWithSize(itemStack, (int) (itemStack.getCount() - inserted));
						if (remainder.getCount() != itemStack.getCount())
							player.getInventory().setItem(i, remainder);
						t.commit();
					}
				}
			});

			if (sendData.booleanValue())
				ToolboxHandler.syncData(player);

		});
		ctx.setPacketHandled(true);
	}

}
