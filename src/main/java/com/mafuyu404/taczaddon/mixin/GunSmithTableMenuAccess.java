package com.mafuyu404.taczaddon.mixin;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = GunSmithTableMenu.class, remap = false)
public interface GunSmithTableMenuAccess {
    @Invoker("getRecipe") GunSmithTableRecipe taczaddon$invokeGetRecipe(ResourceLocation id, RecipeManager manager);
}
