package com.simibubi.create.content.contraptions.components.flywheel.engine;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.item.BlockUseBypassingItem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;

public abstract class EngineBlock extends HorizontalDirectionalBlock implements IWrenchable, BlockUseBypassingItem {

	protected EngineBlock(Properties builder) {
		super(builder);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
		return isValidPosition(state, worldIn, pos, state.getValue(FACING));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getClickedFace();
		return defaultBlockState().setValue(FACING,
				facing.getAxis().isVertical() ? context.getHorizontalDirection().getOpposite() : facing);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		if (worldIn.isClientSide)
			return;

		if (fromPos.equals(getBaseBlockPos(state, pos))) {
			if (!canSurvive(state, worldIn, pos)) {
				worldIn.destroyBlock(pos, true);
				return;
			}
		}
	}

	private boolean isValidPosition(BlockState state, BlockGetter world, BlockPos pos, Direction facing) {
		BlockPos baseBlockPos = getBaseBlockPos(state, pos);
		if (!isValidBaseBlock(world.getBlockState(baseBlockPos), world, pos))
			return false;
		for (Direction otherFacing : Iterate.horizontalDirections) {
			if (otherFacing == facing)
				continue;
			BlockPos otherPos = baseBlockPos.relative(otherFacing);
			BlockState otherState = world.getBlockState(otherPos);
			if (otherState.getBlock() instanceof EngineBlock
					&& getBaseBlockPos(otherState, otherPos).equals(baseBlockPos))
				return false;
		}

		return true;
	}

	public static BlockPos getBaseBlockPos(BlockState state, BlockPos pos) {
		return pos.relative(state.getValue(FACING).getOpposite());
	}

	@Nullable
	@Environment(EnvType.CLIENT)
	public abstract PartialModel getFrameModel();

	protected abstract boolean isValidBaseBlock(BlockState baseBlock, BlockGetter world, BlockPos pos);

	@Override
	public boolean shouldBypass(BlockState state, BlockPos pos, Level level, Player player, InteractionHand hand) {
		return isValidBaseBlock(state, level, pos);
	}
}
