package com.simibubi.create.foundation.tileEntity.behaviour.filtering;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.logistics.item.filter.FilterItem;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FilteringHandler {

	public static InteractionResult onBlockActivated(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
//		Level world = event.getWorld();
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Player player = event.getPlayer();
//		InteractionHand hand = event.getHand();

		if (player.isShiftKeyDown() || player.isSpectator())
			return InteractionResult.PASS;

		FilteringBehaviour behaviour = TileEntityBehaviour.get(world, pos, FilteringBehaviour.TYPE);
		if (behaviour == null)
			return InteractionResult.PASS;

		BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null)
			return InteractionResult.PASS;
		if (behaviour instanceof SidedFilteringBehaviour) {
			behaviour = ((SidedFilteringBehaviour) behaviour).get(ray.getDirection());
			if (behaviour == null)
				return InteractionResult.PASS;
		}
		if (!behaviour.isActive())
			return InteractionResult.PASS;
		if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided)
			((Sided) behaviour.slotPositioning).fromSide(ray.getDirection());
		if (!behaviour.testHit(ray.getLocation()))
			return InteractionResult.PASS;

		ItemStack toApply = player.getItemInHand(hand)
			.copy();

		if (AllTags.AllItemTags.WRENCHES.matches(toApply))
			return InteractionResult.PASS;
		if (AllBlocks.MECHANICAL_ARM.isIn(toApply))
			return InteractionResult.PASS;

		if (!world.isClientSide()) {
			if (!player.isCreative()) {
				if (toApply.getItem() instanceof FilterItem)
					player.getItemInHand(hand)
						.shrink(1);
				if (behaviour.getFilter()
					.getItem() instanceof FilterItem)
					player.getInventory().placeItemBackInInventory(behaviour.getFilter());
			}
			if (toApply.getItem() instanceof FilterItem)
				toApply.setCount(1);
			behaviour.setFilter(toApply);

		} else {
			ItemStack filter = behaviour.getFilter();
			String feedback = "apply_click_again";
			if (toApply.getItem() instanceof FilterItem || !behaviour.isCountVisible())
				feedback = "apply";
			else if (ItemHandlerHelper.canItemStacksStack(toApply, filter))
				feedback = "apply_count";
			String translationKey = world.getBlockState(pos)
				.getBlock()
				.getDescriptionId();
			Component formattedText = new TranslatableComponent(translationKey);
			player.displayClientMessage(Lang.createTranslationTextComponent("logistics.filter." + feedback, formattedText)
				.withStyle(ChatFormatting.WHITE), true);
		}

//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
		return InteractionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	public static boolean onScroll(double delta) {
		HitResult objectMouseOver = Minecraft.getInstance().hitResult;
		if (!(objectMouseOver instanceof BlockHitResult))
			return false;

		BlockHitResult result = (BlockHitResult) objectMouseOver;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		BlockPos blockPos = result.getBlockPos();

		FilteringBehaviour filtering = TileEntityBehaviour.get(world, blockPos, FilteringBehaviour.TYPE);
		if (filtering == null)
			return false;
		if (mc.player.isShiftKeyDown())
			return false;
		if (!mc.player.mayBuild())
			return false;
		if (!filtering.isCountVisible())
			return false;
		if (!filtering.isActive())
			return false;
		if (filtering.slotPositioning instanceof ValueBoxTransform.Sided)
			((Sided) filtering.slotPositioning).fromSide(result.getDirection());
		if (!filtering.testHit(objectMouseOver.getLocation()))
			return false;

		ItemStack filterItem = filtering.getFilter();
		filtering.ticksUntilScrollPacket = 10;
		int maxAmount = (filterItem.getItem() instanceof FilterItem) ? 64 : filterItem.getMaxStackSize();
		int prev = filtering.scrollableValue;
		filtering.scrollableValue =
			(int) Mth.clamp(filtering.scrollableValue + delta * (AllKeys.ctrlDown() ? 16 : 1), 0, maxAmount);

		if (prev != filtering.scrollableValue) {
			float pitch = (filtering.scrollableValue) / (float) (maxAmount);
			pitch = Mth.lerp(pitch, 1.5f, 2f);
			AllSoundEvents.SCROLL_VALUE.play(world, mc.player, blockPos, 1, pitch);
		}

		return true;
	}

}
