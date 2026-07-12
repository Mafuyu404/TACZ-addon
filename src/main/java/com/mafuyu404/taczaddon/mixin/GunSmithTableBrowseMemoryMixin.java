package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers browse position without depending on synthetic button-lambda
 * descriptors. Every TaCZ browse action mutates its fields and then calls
 * init(), so init HEAD is the stable save boundary.
 */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableBrowseMemoryMixin
        extends AbstractContainerScreen<GunSmithTableMenu> {

    @Shadow
    @Nullable
    private GunSmithTableRecipe selectedRecipe;

    @Shadow
    private List<ResourceLocation> selectedRecipeList;

    @Shadow
    private int indexPage;

    @Shadow
    @Nullable
    private ResourceLocation selectedType;

    @Shadow
    @Final
    private Map<ResourceLocation, List<ResourceLocation>> recipes;

    @Shadow
    @Final
    private LinkedHashMap<ResourceLocation, ?> recipeKeys;

    @Shadow
    private int typePage;

    @Shadow
    @Nullable
    private GunSmithTableRecipe getSelectedRecipe(
            ResourceLocation recipeId
    ) {
        throw new AssertionError();
    }

    @Shadow
    private void getPlayerIngredientCount(
            GunSmithTableRecipe recipe
    ) {
        throw new AssertionError();
    }

    @Unique
    private boolean taczaddon$browseStateRestored;

    protected GunSmithTableBrowseMemoryMixin(
            GunSmithTableMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    private void taczaddon$saveBeforeReinit(CallbackInfo ci) {
        if (this.taczaddon$browseStateRestored) {
            this.taczaddon$saveCurrentState();
        }

        this.taczaddon$browseStateRestored = false;
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/gui/GunSmithTableScreen;updateSelectedRecipeAfterFiltering()V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void taczaddon$restoreAfterRecipeClassification(
            CallbackInfo ci
    ) {
        if (this.taczaddon$browseStateRestored) {
            return;
        }

        this.taczaddon$browseStateRestored = true;

        ResourceLocation tableDefinitionId = this.menu.getBlockId();
        BetterGunSmithTable.getBrowseState(tableDefinitionId)
                .ifPresent(this::taczaddon$applyBrowseState);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void taczaddon$saveAfterInit(CallbackInfo ci) {
        this.taczaddon$saveCurrentState();
    }

    @Unique
    private void taczaddon$applyBrowseState(
            BetterGunSmithTable.BrowseState state
    ) {
        ResourceLocation savedType = state.selectedType();

        if (savedType != null && this.recipes.containsKey(savedType)) {
            this.selectedType = savedType;
            this.selectedRecipeList = this.recipes.get(savedType);
        }

        this.typePage = this.taczaddon$typePageFor(
                this.selectedType,
                state.typePage()
        );

        if (this.selectedRecipeList == null
                || this.selectedRecipeList.isEmpty()) {
            this.indexPage = 0;
            this.selectedRecipe = null;
            return;
        }

        int maxRecipePage =
                (this.selectedRecipeList.size() - 1) / 6;

        ResourceLocation savedRecipeId =
                state.selectedRecipeId();
        int savedRecipeIndex = savedRecipeId == null
                ? -1
                : this.selectedRecipeList.indexOf(savedRecipeId);

        if (savedRecipeIndex >= 0) {
            this.indexPage = savedRecipeIndex / 6;
            this.selectedRecipe =
                    this.getSelectedRecipe(savedRecipeId);
        } else {
            this.indexPage = taczaddon$clamp(
                    state.indexPage(),
                    0,
                    maxRecipePage
            );

            int fallbackIndex = Math.min(
                    this.indexPage * 6,
                    this.selectedRecipeList.size() - 1
            );

            this.selectedRecipe = this.getSelectedRecipe(
                    this.selectedRecipeList.get(fallbackIndex)
            );
        }

        if (this.selectedRecipe == null) {
            this.indexPage = 0;
            this.selectedRecipe = this.getSelectedRecipe(
                    this.selectedRecipeList.get(0)
            );
        }

        if (this.selectedRecipe != null) {
            this.getPlayerIngredientCount(this.selectedRecipe);
        }
    }

    @Unique
    private int taczaddon$typePageFor(
            @Nullable ResourceLocation type,
            int fallbackPage
    ) {
        int maxTypePage = this.recipeKeys.isEmpty()
                ? 0
                : (this.recipeKeys.size() - 1) / 7;

        if (type == null) {
            return taczaddon$clamp(
                    fallbackPage,
                    0,
                    maxTypePage
            );
        }

        int index = 0;
        for (ResourceLocation recipeType
                : this.recipeKeys.keySet()) {
            if (type.equals(recipeType)) {
                return index / 7;
            }
            index++;
        }

        return taczaddon$clamp(
                fallbackPage,
                0,
                maxTypePage
        );
    }

    @Unique
    private void taczaddon$saveCurrentState() {
        ResourceLocation tableDefinitionId = this.menu.getBlockId();
        if (tableDefinitionId == null) {
            return;
        }

        ResourceLocation selectedRecipeId =
                this.selectedRecipe == null
                        ? null
                        : this.selectedRecipe.getId();

        BetterGunSmithTable.saveBrowseState(
                tableDefinitionId,
                this.selectedType,
                selectedRecipeId,
                this.typePage,
                this.indexPage
        );
    }

    @Unique
    private static int taczaddon$clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
