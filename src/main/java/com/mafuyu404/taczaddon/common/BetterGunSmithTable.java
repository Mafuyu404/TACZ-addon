package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.*;

public class BetterGunSmithTable {
    private static ResourceLocation recipeId;

    public static ResourceLocation storeRecipeId(ResourceLocation id) {
        recipeId = id;
        return id;
    }
    public static String controlRecipes(String groupName, String selectedAttachmentProp) {
        if (!Config.enableBetterGunSmithTable()) return groupName;
        if (ModList.get().isLoaded("tacztweaks")) return groupName;
        Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
        Player player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getOffhandItem();
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (!recipeId.toString().contains("/") && (iGun == null || Objects.equals(selectedAttachmentProp, "选择属性"))) return groupName;
        ResourceLocation itemId = ResourceLocation.tryParse(recipeId.toString().split(":")[0] + ":" + recipeId.toString().split("/")[1]);
        if (iGun != null) {
            ResourceLocation gunId = iGun.getGunId(gunItem);
            boolean isAmmo = TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(itemId)).orElse(false);
            boolean isAttachment = AllowAttachmentTagMatcher.match(gunId, itemId);
            if (!isAmmo && !isAttachment) {
                return "hidden";
            }
        }
//        if (data != null && !Objects.equals(selectedAttachmentProp, "gui.taczaddon.gun_smith_table.default_prop")) {
//            String prop = Component.translatable(selectedAttachmentProp).getString().replace("+ ", "");
//            HashMap<String, String> AttachmentData = (HashMap<String, String>) data;
//            if (AttachmentData.get(itemId.toString()) == null) return "hidden";
//            if (!AttachmentData.get(itemId.toString()).contains(prop)) {
//                return "hidden";
//            }
//        }
        return groupName;
    }
}
