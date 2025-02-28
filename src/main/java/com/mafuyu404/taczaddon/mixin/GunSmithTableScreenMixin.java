package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public class GunSmithTableScreenMixin {
    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
    @Shadow @Final private List<String> recipeKeys;

    @Unique
    private ResourceLocation TACZ_addon$id;

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private ResourceLocation readId(ResourceLocation id) {
        this.TACZ_addon$id = id;
        return id;
    }

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private String controlRecipes(String groupName) {
        ItemStack gunItem = Minecraft.getInstance().player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            ResourceLocation gunId = iGun.getGunId(gunItem);
            ResourceLocation itemId = ResourceLocation.tryParse("tacz:" + this.TACZ_addon$id.toString().split("/")[1]);
            boolean isAmmo = TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(itemId)).orElse(false);
            boolean isAttachment = AllowAttachmentTagMatcher.match(gunId, itemId);
            if (!isAmmo && !isAttachment) {
                return "hidden";
            }
        }
        return groupName;
    }

    @ModifyVariable(method = "addTypeButtons", at = @At("STORE"), ordinal = 1)
    private int filterType(int typeIndex) {
        ItemStack gunItem = Minecraft.getInstance().player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) return typeIndex;
        if (typeIndex == 0) {
            ArrayList<String> showTypes = new ArrayList<>();
            ArrayList<String> emptyTypes = new ArrayList<>();
            for (String recipeKey : this.recipeKeys) {
                if (this.recipes.get(recipeKey).isEmpty()) emptyTypes.add(recipeKey);
                else showTypes.add(recipeKey);
            }
            for (int i = 0; i < showTypes.size(); i++) {
                this.recipeKeys.set(i, showTypes.get(i));
            }
            for (int i = 0; i < emptyTypes.size(); i++) {
                this.recipeKeys.set(i + showTypes.size(), emptyTypes.get(i));
            }
        }
        String type = this.recipeKeys.get(typeIndex);
        List<ResourceLocation> recipes = this.recipes.get(type);
        if (recipes.isEmpty()) return this.recipes.size() + 100;
        return typeIndex;
    }
}
