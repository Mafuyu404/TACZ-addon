package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(value = GunSmithTableMenu.class, remap = false)
public interface GunSmithTableMenuAccess {
    @Invoker("getRecipe")
    @Nullable
    GunSmithTableRecipe taczaddon$invokeGetRecipe(
            ResourceLocation recipeId,
            RecipeManager recipeManager
    );
}
