package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.network.ContainerPositionPacket;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    @Shadow @Final private Map<String, List<ResourceLocation>> recipes;
    @Shadow @Final private List<String> recipeKeys;

    @Shadow @Nullable private GunSmithTableRecipe selectedRecipe;

    @Shadow private String selectedType;

    @Shadow private List<ResourceLocation> selectedRecipeList;
    @Shadow private int indexPage;

    public GunSmithTableScreenMixin(GunSmithTableMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
    }

    @Shadow protected abstract void getPlayerIngredientCount(GunSmithTableRecipe recipe);

    @Shadow @Nullable protected abstract GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId);

    @Shadow private int typePage;

    @Shadow public abstract void updateIngredientCount();

    private boolean req = false;

    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private ResourceLocation readId(ResourceLocation id) {
        return BetterGunSmithTable.storeRecipeId(id);
    }

    @Unique
    private ArrayList<String> tACZ_addon$AttachmentProp = new ArrayList<>();
    @Unique
    private int tACZ_addon$selectedAttachmentPropIndex = 0;
    @Unique DropDown tACZ_addon$dropdown = null;
    @ModifyVariable(method = "classifyRecipes", at = @At("STORE"), ordinal = 0)
    private String controlRecipes(String groupName) {
        return BetterGunSmithTable.controlRecipes(groupName, "选择属性");
    }

    @ModifyVariable(method = "addTypeButtons", at = @At("STORE"), ordinal = 1)
    private int filterType(int typeIndex) {
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
    private void onInit(CallbackInfo ci) {
        if (tACZ_addon$selectedAttachmentPropIndex != 0) ((VirtualContainerLoader) this).refreshRecipes(tACZ_addon$AttachmentProp.get(tACZ_addon$selectedAttachmentPropIndex), false);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
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
        if (tACZ_addon$dropdown != null) {
            tACZ_addon$selectedAttachmentPropIndex = tACZ_addon$dropdown.getSelected();
        }
        tACZ_addon$dropdown = new DropDown(leftPos - 64, topPos - 16, 64);
        for (String prop : tACZ_addon$AttachmentProp) {
            tACZ_addon$dropdown.addOption(Component.literal(prop));
        }
        tACZ_addon$dropdown.setSelected(tACZ_addon$selectedAttachmentPropIndex);
        this.addRenderableWidget(tACZ_addon$dropdown);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onScreenLoad(CallbackInfo ci) {
        HashMap<String, String> StoredAttachmentData = new HashMap<>();
        TimelessAPI.getAllClientAttachmentIndex().forEach(entry -> {
            StringBuilder data = new StringBuilder();
            entry.getValue().getData().getModifier().forEach((s, jsonProperty) -> {
                jsonProperty.getComponents().forEach(component -> data.append(component.getString()));
            });
            StoredAttachmentData.put(entry.getKey().toString(), data.toString());
        });
        if (DataStorage.get("BetterGunSmithTable.storedAttachmentData") == null) DataStorage.set("BetterGunSmithTable.storedAttachmentData", StoredAttachmentData);

        tACZ_addon$AttachmentProp.add("选择属性");
        tACZ_addon$AttachmentProp.add("开镜时间");
        tACZ_addon$AttachmentProp.add("优势射程");
        tACZ_addon$AttachmentProp.add("竖直后座力");
        tACZ_addon$AttachmentProp.add("水平后座力");
        tACZ_addon$AttachmentProp.add("爆头倍率");
        tACZ_addon$AttachmentProp.add("伤害");
        tACZ_addon$AttachmentProp.add("射速");
        tACZ_addon$AttachmentProp.add("弹速");
        tACZ_addon$AttachmentProp.add("瞄准精度");
        tACZ_addon$AttachmentProp.add("腰射精度");
        tACZ_addon$AttachmentProp.add("声音范围");
        tACZ_addon$AttachmentProp.add("消音");
        tACZ_addon$AttachmentProp.add("点燃实体");
        tACZ_addon$AttachmentProp.add("穿甲倍率");
        tACZ_addon$AttachmentProp.add("穿透");
        tACZ_addon$AttachmentProp.add("爆炸");

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

        this.updateIngredientCount(); // 替代init用
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
        this.tACZ_addon$mouse = new int[] { mouseX, mouseY };
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
    private int tACZ_addon$massCount = 0;
    @Unique
    private Button.OnPress tACZ_addon$onCraft;
    @ModifyArg(method = "addCraftButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;<init>(IIIIIIILnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/gui/components/Button$OnPress;)V", ordinal = 0), remap = true)
    private Button.OnPress eeeee(Button.OnPress p_94276_) {
        tACZ_addon$onCraft = p_94276_;
        return p_94276_;
    }
    @Inject(method = "lambda$addCraftButton$3", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void massCraft(Button b, CallbackInfo ci) {
        if (!Screen.hasShiftDown()) return;
        tACZ_addon$massCount++;
        if (tACZ_addon$massCount >= Config.getMassCraftTime()) {
            tACZ_addon$massCount = 0;
            return;
        }
        tACZ_addon$onCraft.onPress(b);
    }
}
