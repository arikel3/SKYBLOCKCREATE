package com.simibubi.create.compat.rei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.rei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.rei.category.animations.AnimatedMixer;
import com.simibubi.create.content.contraptions.processing.BasinRecipe;
import com.simibubi.create.content.contraptions.processing.HeatCondition;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class MixingCategory extends BasinCategory {

	private final AnimatedMixer mixer = new AnimatedMixer();
	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();
	MixingType type;

	enum MixingType {
		AUTO_SHAPELESS, MIXING, AUTO_BREWING;
	}

	public static MixingCategory autoShapeless() {
		return new MixingCategory(MixingType.AUTO_SHAPELESS, Items.CRAFTING_TABLE, 90);
	}

	public static MixingCategory standard() {
		return new MixingCategory(MixingType.MIXING, AllBlocks.BASIN.get(), 108);
	}

	public static MixingCategory autoBrewing() {
		return new MixingCategory(MixingType.AUTO_BREWING, Blocks.BREWING_STAND, 108);
	}

	protected MixingCategory(MixingType type, ItemLike secondaryItem, int height) {
		super(type != MixingType.AUTO_SHAPELESS, doubleItemIcon(AllBlocks.MECHANICAL_MIXER, () -> secondaryItem),
			emptyBackground(177, height));
		this.type = type;
	}

	@Override
	public void draw(BasinRecipe recipe, PoseStack matrixStack, double mouseX, double mouseY) {
		super.draw(recipe, matrixStack, mouseX, mouseY);
		HeatCondition requiredHeat = recipe.getRequiredHeat();
		if (requiredHeat != HeatCondition.NONE)
			heater.withHeat(requiredHeat.visualizeAsBlazeBurner())
				.draw(matrixStack, getDisplayWidth(null) / 2 + 3, 55);
		mixer.draw(matrixStack, getDisplayWidth(null) / 2 + 3, 34);
	}

}
