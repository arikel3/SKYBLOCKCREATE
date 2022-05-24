package com.simibubi.create.content.contraptions.processing;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ProcessingRecipeSerializer<T extends ProcessingRecipe<?>>
	implements RecipeSerializer<T> {

	private final ProcessingRecipeFactory<T> factory;

	public ProcessingRecipeSerializer(ProcessingRecipeFactory<T> factory) {
		this.factory = factory;
	}

	protected void writeToJson(JsonObject json, T recipe) {
		JsonArray jsonIngredients = new JsonArray();
		JsonArray jsonOutputs = new JsonArray();

		recipe.getIngredients()
			.forEach(i -> jsonIngredients.add(i.toJson()));
		recipe.getFluidIngredients()
			.forEach(i -> jsonIngredients.add(i.serialize()));

		recipe.getRollableResults()
			.forEach(o -> jsonOutputs.add(o.serialize()));
		recipe.getFluidResults()
			.forEach(o -> jsonOutputs.add(FluidHelper.serializeFluidStack(o)));

		json.add("ingredients", jsonIngredients);
		json.add("results", jsonOutputs);

		int processingDuration = recipe.getProcessingDuration();
		if (processingDuration > 0)
			json.addProperty("processingTime", processingDuration);

		HeatCondition requiredHeat = recipe.getRequiredHeat();
		if (requiredHeat != HeatCondition.NONE)
			json.addProperty("heatRequirement", requiredHeat.serialize());

		recipe.writeAdditional(json);
	}

	protected T readFromJson(ResourceLocation recipeId, JsonObject json) {
		ProcessingRecipeBuilder<T> builder = new ProcessingRecipeBuilder<>(factory, recipeId);
		NonNullList<Ingredient> ingredients = NonNullList.create();
		NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
		NonNullList<ProcessingOutput> results = NonNullList.create();
		NonNullList<FluidStack> fluidResults = NonNullList.create();

		for (JsonElement je : GsonHelper.getAsJsonArray(json, "ingredients")) {
			if (FluidIngredient.isFluidIngredient(je))
				fluidIngredients.add(FluidIngredient.deserialize(je));
			else
				ingredients.add(Ingredient.fromJson(je));
		}

		for (JsonElement je : GsonHelper.getAsJsonArray(json, "results")) {
			JsonObject jsonObject = je.getAsJsonObject();
			if (GsonHelper.isValidNode(jsonObject, "fluid"))
				fluidResults.add(FluidHelper.deserializeFluidStack(jsonObject));
			else
				results.add(ProcessingOutput.deserialize(je));
		}

		builder.withItemIngredients(ingredients)
			.withItemOutputs(results)
			.withFluidIngredients(fluidIngredients)
			.withFluidOutputs(fluidResults);

		if (GsonHelper.isValidNode(json, "processingTime"))
			builder.duration(GsonHelper.getAsInt(json, "processingTime"));
		if (GsonHelper.isValidNode(json, "heatRequirement"))
			builder.requiresHeat(HeatCondition.deserialize(GsonHelper.getAsString(json, "heatRequirement")));

		T recipe = builder.build();
		recipe.readAdditional(json);
		return recipe;
	}

	protected void writeToBuffer(FriendlyByteBuf buffer, T recipe) {
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		NonNullList<FluidIngredient> fluidIngredients = recipe.getFluidIngredients();
		NonNullList<ProcessingOutput> outputs = recipe.getRollableResults();
		NonNullList<FluidStack> fluidOutputs = recipe.getFluidResults();

		buffer.writeVarInt(ingredients.size());
		ingredients.forEach(i -> i.toNetwork(buffer));
		buffer.writeVarInt(fluidIngredients.size());
		fluidIngredients.forEach(i -> i.write(buffer));

		buffer.writeVarInt(outputs.size());
		outputs.forEach(o -> o.write(buffer));
		buffer.writeVarInt(fluidOutputs.size());
		fluidOutputs.forEach(o -> o.toBuffer(buffer));

		buffer.writeVarInt(recipe.getProcessingDuration());
		buffer.writeVarInt(recipe.getRequiredHeat()
			.ordinal());

		recipe.writeAdditional(buffer);
	}

	protected T readFromBuffer(ResourceLocation recipeId, FriendlyByteBuf buffer) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
		NonNullList<ProcessingOutput> results = NonNullList.create();
		NonNullList<FluidStack> fluidResults = NonNullList.create();

		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			ingredients.add(Ingredient.fromNetwork(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidIngredients.add(FluidIngredient.read(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			results.add(ProcessingOutput.read(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidResults.add(FluidStack.fromBuffer(buffer));

		T recipe = new ProcessingRecipeBuilder<>(factory, recipeId).withItemIngredients(ingredients)
			.withItemOutputs(results)
			.withFluidIngredients(fluidIngredients)
			.withFluidOutputs(fluidResults)
			.duration(buffer.readVarInt())
			.requiresHeat(HeatCondition.values()[buffer.readVarInt()])
			.build();
		recipe.readAdditional(buffer);
		return recipe;
	}

	public final void write(JsonObject json, T recipe) {
		writeToJson(json, recipe);
	}

	@Override
	public final T fromJson(ResourceLocation id, JsonObject json) {
		return readFromJson(id, json);
	}

	@Override
	public final void toNetwork(FriendlyByteBuf buffer, T recipe) {
		writeToBuffer(buffer, recipe);
	}

	@Override
	public final T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
		return readFromBuffer(id, buffer);
	}

	public ProcessingRecipeFactory<T> getFactory() {
		return factory;
	}

}
