package com.simibubi.create;

import java.util.HashMap;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.components.actors.BellMovementBehaviour;
import com.simibubi.create.content.contraptions.components.actors.CampfireMovementBehaviour;
import com.simibubi.create.content.contraptions.components.actors.dispenser.DispenserMovementBehaviour;
import com.simibubi.create.content.contraptions.components.actors.dispenser.DropperMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AllMovementBehaviours {
	private static final HashMap<ResourceLocation, MovementBehaviour> MOVEMENT_BEHAVIOURS = new HashMap<>();

	public static void addMovementBehaviour(ResourceLocation resourceLocation, MovementBehaviour movementBehaviour) {
		if (MOVEMENT_BEHAVIOURS.containsKey(resourceLocation))
			Create.LOGGER.warn("Movement behaviour for " + resourceLocation.toString() + " was overridden");
		MOVEMENT_BEHAVIOURS.put(resourceLocation, movementBehaviour);
	}

	public static void addMovementBehaviour(Block block, MovementBehaviour movementBehaviour) {
		addMovementBehaviour(Registry.BLOCK.getKey(block), movementBehaviour);
	}

	@Nullable
	public static MovementBehaviour of(ResourceLocation resourceLocation) {
		return MOVEMENT_BEHAVIOURS.getOrDefault(resourceLocation, null);
	}

	@Nullable
	public static MovementBehaviour of(Block block) {
		return of(Registry.BLOCK.getKey(block));
	}

	@Nullable
	public static MovementBehaviour of(BlockState state) {
		return of(state.getBlock());
	}

	public static boolean contains(Block block) {
		return MOVEMENT_BEHAVIOURS.containsKey(Registry.BLOCK.getKey(block));
	}

	public static <B extends Block> NonNullConsumer<? super B> addMovementBehaviour(
		MovementBehaviour movementBehaviour) {
		return b -> addMovementBehaviour(Registry.BLOCK.getKey(b), movementBehaviour);
	}

	static void register() {
		addMovementBehaviour(Blocks.BELL, new BellMovementBehaviour());
		addMovementBehaviour(Blocks.CAMPFIRE, new CampfireMovementBehaviour());

		DispenserMovementBehaviour.gatherMovedDispenseItemBehaviours();
		addMovementBehaviour(Blocks.DISPENSER, new DispenserMovementBehaviour());
		addMovementBehaviour(Blocks.DROPPER, new DropperMovementBehaviour());
	}
}
