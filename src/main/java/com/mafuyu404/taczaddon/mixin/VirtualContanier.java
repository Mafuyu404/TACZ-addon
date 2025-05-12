package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.VirtualContainerLoader;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class VirtualContanier implements VirtualContainerLoader {
//    @Shadow protected abstract void classifyRecipes();
//
//    @Shadow private List<ResourceLocation> selectedRecipeList;
//
//    @Shadow @Nullable protected abstract GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId);
//
//    @Shadow public abstract void updateIngredientCount();
//
//    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
//    @Shadow private String selectedType;
    @Unique
    private ArrayList<ItemStack> tACZ_addon$virtualContainer = new ArrayList<>();
//
    @Override
    public void tACZ_addon$setVirtualContanier(ArrayList<ItemStack> items) {
        this.tACZ_addon$virtualContainer = items;
    }
    @Override
    public ArrayList<ItemStack> tACZ_addon$getVirtualContanier() {
        return this.tACZ_addon$virtualContainer;
    }
//
//    public void refreshRecipes(String propKey, boolean refreshPage) {
//        String prop = Component.translatable(propKey).getString().replace("+ ", "");
//        ArrayList<ResourceLocation> result = new ArrayList<>();
//        Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
//        if (data != null && !Objects.equals(prop, "gui.taczaddon.gun_smith_table.default_prop")) {
//            HashMap<String, String> AttachmentData = (HashMap<String, String>) data;
//            this.recipes.get(this.selectedType).forEach(resourceLocation -> {
//                GunSmithTableRecipe recipe = this.getSelectedRecipe(resourceLocation);
//                CompoundTag tag = recipe.getOutput().getTag();
//                if (tag == null) return;
//                String itemId = tag.getString("AttachmentId");
//                if (AttachmentData.get(itemId.toString()) == null) return;
//                if (!AttachmentData.get(itemId.toString()).contains(prop)) {
//                    return;
//                }
//                result.add(resourceLocation);
//            });
//            this.selectedRecipeList = result;
//        }
//        else this.selectedRecipeList = this.recipes.get(this.selectedType);
//        if (refreshPage) this.updateIngredientCount();
//    }
}
