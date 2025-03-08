package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public class GunSmithTableScreenMixin {
    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
    @Shadow @Final private List<String> recipeKeys;

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private ResourceLocation readId114(ResourceLocation id) {
        return BetterGunSmithTable.storeRecipeId(id);
    }
//    @Inject(method = "lambda$classifyRecipes$1", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/crafting/GunSmithTableRecipe;getResult()Lcom/tacz/guns/crafting/GunSmithTableResult;"))
//    private void good(ResourceLocation id, GunSmithTableRecipe recipe, CallbackInfo ci) {
//        BetterGunSmithTable.storeRecipeId(id);
//    }

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private String controlRecipes114(String groupName) {
        return BetterGunSmithTable.controlRecipes(groupName);
    }
//    @ModifyVariable(method = "lambda$classifyRecipes$1", at = @At("STORE"), ordinal = 0)
//    private String controlRecipes103(String groupName) {
//        return BetterGunSmithTable.controlRecipes(groupName);
//    }

    @ModifyVariable(method = "addTypeButtons", at = @At("STORE"), ordinal = 1)
    private int filterType(int typeIndex) {
        if (Objects.equals(ModList.get().getModContainerById("tacz").get().getModInfo().getVersion().toString(), "1.0.3")) return typeIndex;
        Player player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getOffhandItem();
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) gunItem = player.getMainHandItem();
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
