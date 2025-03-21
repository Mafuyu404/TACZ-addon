package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.network.ContainerPositionPacket;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableScreenMixin {
    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
    @Shadow @Final private List<String> recipeKeys;

    @Shadow @Nullable private GunSmithTableRecipe selectedRecipe;

    @Shadow private String selectedType;

    @Shadow private List<ResourceLocation> selectedRecipeList;
    @Shadow private int indexPage;

    @Shadow protected abstract void getPlayerIngredientCount(GunSmithTableRecipe recipe);

    @Shadow @Nullable protected abstract GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId);

    @Shadow private int typePage;

    @Shadow public abstract void updateIngredientCount();

    private boolean req = false;

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
        if (!Config.enableBetterGunSmithTable()) return typeIndex;
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

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    private void onScreenChanged(CallbackInfo ci) {
        DataStorage.set("BetterGunSmithTable.storedType", this.selectedType);
        DataStorage.set("BetterGunSmithTable.storedTypePage", this.typePage);
        DataStorage.set("BetterGunSmithTable.storedIndexPage", this.indexPage);
        DataStorage.set("BetterGunSmithTable.storedRecipe", this.selectedRecipe);
        if (!Config.enableGunSmithTableContainerReader()) return;
        if (!req) {
            BlockPos blockPos = (BlockPos) DataStorage.get("BetterGunSmithTable.interactBlockPos");
            NetworkHandler.CHANNEL.sendToServer(new ContainerPositionPacket(blockPos));
            req = true;
        }
        else req = false;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onScreenLoad(CallbackInfo ci) {
        if (!Config.enableGunSmithTableMemory()) return;

        if (DataStorage.get("BetterGunSmithTable.storedTypePage") == null) DataStorage.set("BetterGunSmithTable.storedTypePage", 0);
        int storedTypePage = (int) DataStorage.get("BetterGunSmithTable.storedTypePage");

        if (DataStorage.get("BetterGunSmithTable.storedType") == null) DataStorage.set("BetterGunSmithTable.storedType", "ammo");
        final String[] storedType = {(String) DataStorage.get("BetterGunSmithTable.storedType")};

        final int[] showTypeCount = {0};
        this.recipes.forEach((type, recipes) -> {
            if (!recipes.isEmpty()) {
                showTypeCount[0]++;
                if (this.recipes.get(storedType[0]).isEmpty()) storedType[0] = type;
            }
        });
        this.selectedRecipeList = this.recipes.get(storedType[0]);
        if (showTypeCount[0] < 8)  storedTypePage = 0;

        if (DataStorage.get("BetterGunSmithTable.storedIndexPage") == null) DataStorage.set("BetterGunSmithTable.storedIndexPage", 0);
        int storedIndexPage = (int) DataStorage.get("BetterGunSmithTable.storedIndexPage");

        if (this.selectedRecipeList.size() <= storedIndexPage * 6) storedIndexPage = 0;

        if (DataStorage.get("BetterGunSmithTable.storedRecipe") == null) DataStorage.set("BetterGunSmithTable.storedRecipe", this.getSelectedRecipe(this.selectedRecipeList.get(0)));
        GunSmithTableRecipe storedRecipe = (GunSmithTableRecipe) DataStorage.get("BetterGunSmithTable.storedRecipe");

        if ((this.selectedRecipeList.indexOf(storedRecipe.getId()) >= (storedIndexPage + 1) * 6) || !this.selectedRecipeList.contains(storedRecipe.getId())) storedRecipe = this.getSelectedRecipe(this.selectedRecipeList.get(0));

        this.selectedType = storedType[0];
        this.indexPage = storedIndexPage;
        this.selectedRecipe = storedRecipe;
        this.getPlayerIngredientCount(this.selectedRecipe);
        this.typePage = storedTypePage;
        this.updateIngredientCount();
    }

    @Inject(method = "lambda$addCraftButton$3", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void onCrafted(Button b, CallbackInfo ci) {
        if (Config.enableGunSmithTableCraftToast()) ItemIconToast.create(
            "已制作",
            this.selectedRecipe.getOutput().getHoverName().getString() + " x " + this.selectedRecipe.getOutput().getCount(),
            this.selectedRecipe.getOutput());
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 0), index = 1, remap = true)
    private Component renderPageInfo(Component p_282131_) {
        String type = Component.translatable(String.format("tacz.type.%s.name", this.selectedType)).getString();
        String title = String.format("%s (第%s页-共%s页)", type, this.indexPage + 1, (int) Math.ceil((double) this.selectedRecipeList.size() / 6));
        return Component.translatable(title);
    }

    @ModifyVariable(method = "getPlayerIngredientCount", at = @At("STORE"), ordinal = 0)
    private Inventory modifyIngredientShow(Inventory inventory) {
        ArrayList<ItemStack> items = ((VirtualContainerLoader) this).tACZ_addon$getVirtualContanier();
        VirtualInventory virtualInventory = new VirtualInventory(inventory.getContainerSize() + items.size(), inventory.player);
        virtualInventory.extend();
        for (int i = 0; i < items.size(); i++) {
            virtualInventory.setItem(virtualInventory.playerInventorySize + i, items.get(i));
        }
        return virtualInventory;
    }

    @Unique
    private int[] tACZ_addon$mouse;
    @Unique
    private Font tACZ_addon$font = Minecraft.getInstance().font;
    @Unique
    private HashMap<String, Boolean> tACZ_addon$hover = new HashMap<>();
    @Inject(method = "render", at = @At("HEAD"), remap = true)
    private void storedGraphics(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.tACZ_addon$setMouse(new int[] { mouseX, mouseY });
    }
    @ModifyArg(method = "lambda$render$16", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private Font fontfontfont(Font p_282308_) {
        this.tACZ_addon$font = p_282308_;
        return p_282308_;
    }
    @Redirect(method = "renderIngredient", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"), remap = true)
    private void renderBetterIngredient(GuiGraphics graphics, ItemStack itemStack, int x, int y) {
        graphics.renderItem(itemStack, x, y);
        if (tACZ_addon$isHovering(x, y)) {
            graphics.renderTooltip(tACZ_addon$font, itemStack, tACZ_addon$mouse[0], tACZ_addon$mouse[1]);
            DataStorage.set("GunSmithTableJEI", itemStack);
            this.tACZ_addon$hover.put(x + String.valueOf(y), true);
        }
        else {
            this.tACZ_addon$hover.put(x + String.valueOf(y), false);
            if (this.tACZ_addon$hover.values().stream().filter(Boolean::booleanValue).toArray().length == 0) {
                DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
            }
        }
    }

    @Unique
    private boolean tACZ_addon$isHovering(int x, int y) {
        return tACZ_addon$mouse[0] >= x && tACZ_addon$mouse[0] <= x + 16 && tACZ_addon$mouse[1] >= y && tACZ_addon$mouse[1] <= y + 16;
    }

    @Unique
    public void tACZ_addon$setMouse(int[] mouse) {
        this.tACZ_addon$mouse = mouse;
    }
}
