package com.mafuyu404.taczaddon.init;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class DataDrivenSmithingRecipeSerializer implements RecipeSerializer<DataDrivenSmithingRecipe> {
    public static final DataDrivenSmithingRecipeSerializer INSTANCE = new DataDrivenSmithingRecipeSerializer();

    @Override
    public DataDrivenSmithingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        Ingredient template = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "template"));
        Ingredient base = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "base"));
        Ingredient addition = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "addition"));

        return new DataDrivenSmithingRecipe(recipeId, template, base, addition);
    }

    @Override
    public DataDrivenSmithingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        Ingredient template = Ingredient.fromNetwork(buffer);
        Ingredient base = Ingredient.fromNetwork(buffer);
        Ingredient addition = Ingredient.fromNetwork(buffer);

        return new DataDrivenSmithingRecipe(recipeId, template, base, addition);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, DataDrivenSmithingRecipe recipe) {
        recipe.getTemplate().toNetwork(buffer);
        recipe.getBase().toNetwork(buffer);
        recipe.getAddition().toNetwork(buffer);
    }
}
