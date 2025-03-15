package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BetterGunSmithTable {
//    private static final List<GunSmithTableRecipe> recipeList = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING);
//    private static GunSmithTableRecipe EmptyRecipe;
//    public static GunSmithTableRecipe getEmptyRecipe() {
//        String gunId = Minecraft.getInstance().player.getMainHandItem().getTag().getString("GunId");
//        if (EmptyRecipe == null || !EmptyRecipe.getId().toString().equals(gunId)) {
//            recipeList.forEach(recipe -> {
//                if (recipe.getId().toString().equals(gunId)) {
//                    EmptyRecipe = recipe;
//                }
//            });
//        }
//        return EmptyRecipe;
//    }
    private static ResourceLocation recipeId;

    public static ResourceLocation storeRecipeId(ResourceLocation id) {
        recipeId = id;
        return id;
    }
    public static String controlRecipes(String groupName) {
        if (!Config.enableBetterGunSmithTable()) return groupName;
        Player player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getOffhandItem();
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            ResourceLocation gunId = iGun.getGunId(gunItem);
            ResourceLocation itemId = ResourceLocation.tryParse(recipeId.toString().split(":")[0] + ":" + recipeId.toString().split("/")[1]);
            boolean isAmmo = TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(itemId)).orElse(false);
            boolean isAttachment = AllowAttachmentTagMatcher.match(gunId, itemId);
            if (!isAmmo && !isAttachment) {
                return "hidden";
            }
        }
        return groupName;
    }
    public static int filterType(int typeIndex, List<String> recipeKeys, Map<String, List<ResourceLocation>> recipes) {
        if (!Config.enableBetterGunSmithTable()) return typeIndex;
        Player player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getOffhandItem();
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) return typeIndex;
        if (typeIndex == 0) {
            ArrayList<String> showTypes = new ArrayList<>();
            ArrayList<String> emptyTypes = new ArrayList<>();
            for (String recipeKey : recipeKeys) {
                if (recipes.get(recipeKey).isEmpty()) emptyTypes.add(recipeKey);
                else showTypes.add(recipeKey);
            }
            for (int i = 0; i < showTypes.size(); i++) {
                recipeKeys.set(i, showTypes.get(i));
            }
            for (int i = 0; i < emptyTypes.size(); i++) {
                recipeKeys.set(i + showTypes.size(), emptyTypes.get(i));
            }
        }
        String type = recipeKeys.get(typeIndex);
        List<ResourceLocation> _recipes = recipes.get(type);
        if (recipes.isEmpty()) return _recipes.size() + 100;
        return typeIndex;
    }
}
