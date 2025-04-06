package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.init.DataStorage;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class JeiCompat {
    private static final String MOD_ID = "jei";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean showRecipes(ItemStack itemStack) {
        if (INSTALLED) {
            return JeiPlugin.showRecipes(itemStack);
        }
        return false;
    }
}
