package com.simibubi.create.content.contraptions.components.structureMovement;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import io.github.fabricators_of_create.porting_lib.mixin.client.accessor.KeyMappingAccessor;

import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.content.contraptions.components.structureMovement.sync.ContraptionInteractionPacket;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import io.github.fabricators_of_create.porting_lib.util.EntityHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class ContraptionHandlerClient {

	@Environment(EnvType.CLIENT)
	public static void preventRemotePlayersWalkingAnimations(Player player) {
//		if (event.phase == Phase.START)
//			return;
		if (!(player instanceof RemotePlayer))
			return;
		RemotePlayer remotePlayer = (RemotePlayer) player;
		CompoundTag data = EntityHelper.getExtraCustomData(remotePlayer);
		if (!data.contains("LastOverrideLimbSwingUpdate"))
			return;

		int lastOverride = data.getInt("LastOverrideLimbSwingUpdate");
		data.putInt("LastOverrideLimbSwingUpdate", lastOverride + 1);
		if (lastOverride > 5) {
			data.remove("LastOverrideLimbSwingUpdate");
			data.remove("OverrideLimbSwing");
			return;
		}

		float limbSwing = data.getFloat("OverrideLimbSwing");
		remotePlayer.xo = remotePlayer.getX() - (limbSwing / 4);
		remotePlayer.zo = remotePlayer.getZ();
	}

	@Environment(EnvType.CLIENT)
	public static InteractionResult rightClickingOnContraptionsGetsHandledLocally(InteractionHand hand) {
		Minecraft mc = Minecraft.getInstance();

		if (Minecraft.getInstance().screen != null) // this is the only input event that doesn't check this?
			return InteractionResult.PASS;

		LocalPlayer player = mc.player;
		if (player == null)
			return InteractionResult.PASS;
		if (player.isPassenger())
			return InteractionResult.PASS;
		if (mc.level == null)
			return InteractionResult.PASS;
		if(mc.gameMode == null)
			return InteractionResult.PASS;
//		if (!event.isUseItem())
//			return InteractionResult.PASS;
		Vec3 origin = RaycastHelper.getTraceOrigin(player);

		double reach = ReachEntityAttributes.getReachDistance(player, mc.gameMode.getPickRange());
		if (mc.hitResult != null && mc.hitResult.getLocation() != null)
			reach = Math.min(mc.hitResult.getLocation()
				.distanceTo(origin), reach);

		Vec3 target = RaycastHelper.getTraceTarget(player, reach, origin);
		for (AbstractContraptionEntity contraptionEntity : mc.level
			.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(origin, target))) {

			Vec3 localOrigin = contraptionEntity.toLocalVector(origin, 1);
			Vec3 localTarget = contraptionEntity.toLocalVector(target, 1);
			Contraption contraption = contraptionEntity.getContraption();

			MutableObject<BlockHitResult> mutableResult = new MutableObject<>();
			PredicateTraceResult predicateResult = RaycastHelper.rayTraceUntil(localOrigin, localTarget, p -> {
				StructureBlockInfo blockInfo = contraption.getBlocks()
					.get(p);
				if (blockInfo == null)
					return false;
				BlockState state = blockInfo.state;
				VoxelShape raytraceShape = state.getShape(Minecraft.getInstance().level, BlockPos.ZERO.below());
				if (raytraceShape.isEmpty())
					return false;
				BlockHitResult rayTrace = raytraceShape.clip(localOrigin, localTarget, p);
				if (rayTrace != null) {
					mutableResult.setValue(rayTrace);
					return true;
				}
				return false;
			});

			if (predicateResult == null || predicateResult.missed())
				return InteractionResult.PASS;

			BlockHitResult rayTraceResult = mutableResult.getValue();
			Direction face = rayTraceResult.getDirection();
			BlockPos pos = rayTraceResult.getBlockPos();

//			InteractionHand hand = player.getUsedItemHand();
			if (!contraptionEntity.handlePlayerInteraction(player, pos, face, hand))
				return InteractionResult.PASS;
			AllPackets.channel.sendToServer(new ContraptionInteractionPacket(contraptionEntity, hand, pos, face));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

}
