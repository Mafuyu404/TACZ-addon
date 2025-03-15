package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.client.ClientDataStorage;
import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.ItemIconToast;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableScreenMixin {
    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
    @Shadow @Final private List<String> recipeKeys;

    @Shadow @Nullable private GunSmithTableRecipe selectedRecipe;

    @Shadow private String selectedType;

    @Shadow private List<ResourceLocation> selectedRecipeList;
    @Shadow private int indexPage;

    @Shadow protected abstract void init();

    @Shadow protected abstract void getPlayerIngredientCount(GunSmithTableRecipe recipe);

    @Shadow @Nullable protected abstract GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId);

    @Shadow private int typePage;
    private String storedType = "ammo";

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private ResourceLocation readId(ResourceLocation id) {
        return BetterGunSmithTable.storeRecipeId(id);
    }

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private String controlRecipes(String groupName) {
        return BetterGunSmithTable.controlRecipes(groupName);
    }

    @ModifyVariable(method = "addTypeButtons", at = @At("STORE"), ordinal = 1)
    private int filterType(int typeIndex) {
//        return BetterGunSmithTable.filterType(typeIndex, this.recipeKeys, this.recipes);
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

    @Inject(method = "init", at = @At("HEAD"))
    private void bbb(CallbackInfo ci) {
        ClientDataStorage.set("BetterGunSmithTable.storedType", this.selectedType);
        ClientDataStorage.set("BetterGunSmithTable.storedTypePage", this.typePage);
        ClientDataStorage.set("BetterGunSmithTable.storedIndexPage", this.indexPage);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbc(CallbackInfo ci) {
        if (ClientDataStorage.get("BetterGunSmithTable.storedType") == null) ClientDataStorage.set("BetterGunSmithTable.storedType", "ammo");
        if (ClientDataStorage.get("BetterGunSmithTable.storedTypePage") == null) ClientDataStorage.set("BetterGunSmithTable.storedTypePage", 0);
        if (ClientDataStorage.get("BetterGunSmithTable.storedIndexPage") == null) ClientDataStorage.set("BetterGunSmithTable.storedIndexPage", 0);
        final String[] storedType = {(String) ClientDataStorage.get("BetterGunSmithTable.storedType")};
        int storedTypePage = (int) ClientDataStorage.get("BetterGunSmithTable.storedTypePage");
        int storedIndexPage = (int) ClientDataStorage.get("BetterGunSmithTable.storedIndexPage");
//        System.out.print("\n\n");
//        System.out.print(storedIndexPage);
//        System.out.print("\n\n");
        if (this.recipes.get(storedType[0]).isEmpty()) {
            this.recipes.forEach((s, resourceLocations) -> {
                if (this.recipes.get(storedType[0]).isEmpty() && !resourceLocations.isEmpty()) {
                    storedType[0] = s;
                }
            });
        }
        this.selectedType = storedType[0];
        this.selectedRecipeList = this.recipes.get(storedType[0]);
        this.indexPage = storedIndexPage;
        this.selectedRecipe = this.getSelectedRecipe(this.selectedRecipeList.isEmpty() ? null : this.selectedRecipeList.get(0));
        this.getPlayerIngredientCount(this.selectedRecipe);
        this.typePage = storedTypePage;
        this.init();
    }

    @Inject(method = "lambda$addCraftButton$3", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void iii(Button b, CallbackInfo ci) {
        if (Config.enableGunSmithTableCraftToast()) ItemIconToast.create(
                "已制作",
                this.selectedRecipe.getOutput().getHoverName().getString() + " x " + this.selectedRecipe.getOutput().getCount(),
                this.selectedRecipe.getOutput());
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 0), index = 1)
    private Component aaa(Component p_282131_) {
        String type = Component.translatable(String.format("tacz.type.%s.name", this.selectedType)).getString();
        String title = String.format("%s (第%s页-共%s页)", type, this.indexPage + 1, (int) Math.ceil((double) this.selectedRecipeList.size() / 6));
        return Component.translatable(title);
    }
}
