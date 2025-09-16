package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
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

    public static boolean allowAttachment(ItemStack gunItem, ResourceLocation attachmentId) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        ResourceLocation gunId = null;
        if (iGun != null) {
            gunId = iGun.getGunId(gunItem);
        }
        return AllowAttachmentTagMatcher.match(gunId, attachmentId);
    }

    public static boolean allowAmmo(ItemStack gunItem, ResourceLocation ammoId) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        ResourceLocation gunId = null;
        if (iGun != null) {
            gunId = iGun.getGunId(gunItem);
        }
        return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId)).orElse(false);
    }

    public static boolean isHoldingGun(Player player) {
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) return true;
        return IGun.getIGunOrNull(player.getOffhandItem()) != null;
    }
    public static ItemStack getHoldingGun(Player player) {
        IGun main = IGun.getIGunOrNull(player.getMainHandItem());
        IGun off = IGun.getIGunOrNull(player.getOffhandItem());
        if (main != null) return player.getMainHandItem();
        if (off != null) return player.getOffhandItem();
        return null;
    }

    public static String getTranslationKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }
}
