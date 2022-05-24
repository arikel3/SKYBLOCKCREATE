package com.simibubi.create.content.contraptions.components.actors;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.util.EntityHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SeatMovementBehaviour implements MovementBehaviour {

	@Override
	public void startMoving(MovementContext context) {
		MovementBehaviour.super.startMoving(context);
		int indexOf = context.contraption.getSeats()
			.indexOf(context.localPos);
		context.data.putInt("SeatIndex", indexOf);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		MovementBehaviour.super.visitNewPosition(context, pos);
		AbstractContraptionEntity contraptionEntity = context.contraption.entity;
		if (contraptionEntity == null)
			return;
		int index = context.data.getInt("SeatIndex");
		if (index == -1)
			return;

		Map<UUID, Integer> seatMapping = context.contraption.getSeatMapping();
		BlockState blockState = context.world.getBlockState(pos);
		boolean slab = blockState.getBlock() instanceof SlabBlock && blockState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
		boolean solid = blockState.canOcclude() || slab;

		// Occupied
		if (seatMapping.containsValue(index)) {
			if (!solid)
				return;
			Entity toDismount = null;
			for (Map.Entry<UUID, Integer> entry : seatMapping.entrySet()) {
				if (entry.getValue() != index)
					continue;
				for (Entity entity : contraptionEntity.getPassengers()) {
					if (!entry.getKey()
						.equals(entity.getUUID()))
						continue;
					toDismount = entity;
				}
			}
			if (toDismount != null) {
				toDismount.stopRiding();
				Vec3 position = VecHelper.getCenterOf(pos)
					.add(0, slab ? .5f : 1f, 0);
				toDismount.teleportTo(position.x, position.y, position.z);
				EntityHelper.getExtraCustomData(toDismount)
					.remove("ContraptionDismountLocation");
			}
			return;
		}

		if (solid)
			return;

		List<Entity> nearbyEntities = context.world.getEntitiesOfClass(Entity.class,
			new AABB(pos).deflate(1 / 16f), SeatBlock::canBePickedUp);
		if (!nearbyEntities.isEmpty())
			contraptionEntity.addSittingPassenger(nearbyEntities.get(0), index);
	}

}
