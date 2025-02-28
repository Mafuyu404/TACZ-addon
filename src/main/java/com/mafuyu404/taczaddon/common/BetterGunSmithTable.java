package com.mafuyu404.taczaddon.common;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import net.minecraft.client.Minecraft;

import java.util.List;

public class BetterGunSmithTable {
    private static final List<GunSmithTableRecipe> recipeList = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
    private static GunSmithTableRecipe EmptyRecipe;
    public static GunSmithTableRecipe getEmptyRecipe() {
        String gunId = Minecraft.getInstance().player.getMainHandItem().getTag().getString("GunId");
        if (EmptyRecipe == null || !EmptyRecipe.getId().toString().equals(gunId)) {
            recipeList.forEach(recipe -> {
                if (recipe.getId().toString().equals(gunId)) {
                    EmptyRecipe = recipe;
                }
            });
        }
        return EmptyRecipe;
    }
}
