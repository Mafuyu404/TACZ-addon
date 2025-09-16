package com.mafuyu404.taczaddon.init;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;

public class SmithingRecipeSerializer<T extends SmithingRecipe> implements RecipeSerializer<T> {
    private final SmithingRecipeSerializer.Factory<T> factory;

    public SmithingRecipeSerializer(SmithingRecipeSerializer.Factory<T> factory) {
        this.factory = factory;
    }

    @Override
    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        return this.factory.create(recipeId);
    }

    @Override
    public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        return this.factory.create(recipeId);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, T recipe) {
        // 无需发送额外数据
    }

    public interface Factory<T extends SmithingRecipe> {
        T create(ResourceLocation id);
    }
}
