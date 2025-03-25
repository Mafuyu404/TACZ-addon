package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

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
    public static String controlRecipes(String groupName, String selectedAttachmentProp) {
        if (!Config.enableBetterGunSmithTable()) return groupName;
        Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
        Player player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getOffhandItem();
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) gunItem = player.getMainHandItem();
        ResourceLocation itemId = ResourceLocation.tryParse(recipeId.toString().split(":")[0] + ":" + recipeId.toString().split("/")[1]);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            ResourceLocation gunId = iGun.getGunId(gunItem);
            boolean isAmmo = TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(itemId)).orElse(false);
            boolean isAttachment = AllowAttachmentTagMatcher.match(gunId, itemId);
            if (!isAmmo && !isAttachment) {
                return "hidden";
            }
        }
        if (data != null && !Objects.equals(selectedAttachmentProp, "选择属性")) {
            HashMap<String, String> AttachmentData = (HashMap<String, String>) data;
            if (AttachmentData.get(itemId.toString()) == null) return "hidden";
            if (!AttachmentData.get(itemId.toString()).contains(selectedAttachmentProp)) {
                return "hidden";
            }
        }
        return groupName;
    }
}
