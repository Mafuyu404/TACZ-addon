package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.network.ContainerPositionPacket;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    @Shadow @Nullable private GunSmithTableRecipe selectedRecipe;

    @Shadow private List<ResourceLocation> selectedRecipeList;
    @Shadow private int indexPage;

    @Shadow
    public abstract void updateIngredientCount();

    @Shadow
    private ResourceLocation selectedType;

    @Shadow @Final
    private Map<ResourceLocation, List<ResourceLocation>> recipes;

    @Shadow @Final
    private LinkedHashMap<ResourceLocation, TabConfig> recipeKeys;

    @Shadow
    private int typePage;

    @Shadow
    @Nullable
    private GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId) {
        throw new AssertionError();
    }

    @Shadow
    private void getPlayerIngredientCount(GunSmithTableRecipe recipe) {
        throw new AssertionError();
    }

    public GunSmithTableScreenMixin(GunSmithTableMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
    }

    @Unique
    private boolean tACZ_addon$containerSnapshotRequested = false;

    @Unique
    @Nullable
    private BlockPos tACZ_addon$lastRequestedContainerPos = null;

    @Unique
    private void tACZ_addon$requestNearbyContainerSnapshotIfNeeded() {
        if (!Config.enableGunSmithTableContainerReader()) {
            return;
        }

        Object rawPos = DataStorage.get("BetterGunSmithTable.interactBlockPos");
        if (!(rawPos instanceof BlockPos blockPos)) {
            return;
        }

        if (!blockPos.equals(this.tACZ_addon$lastRequestedContainerPos)) {
            this.tACZ_addon$lastRequestedContainerPos = blockPos;
            this.tACZ_addon$containerSnapshotRequested = false;
        }

        if (this.tACZ_addon$containerSnapshotRequested) {
            return;
        }

        this.tACZ_addon$containerSnapshotRequested = true;
        NetworkHandler.CHANNEL.sendToServer(new ContainerPositionPacket(blockPos));
    }

    @Unique
    private ArrayList<String> tACZ_addon$AttachmentProp = new ArrayList<>();
    @Unique
    private int tACZ_addon$selectedAttachmentPropIndex = 0;
    @Unique DropDown tACZ_addon$dropdown = null;

    @Unique
    private boolean tACZ_addon$browseStateRestored = false;

    @Redirect(method = "classifyRecipes", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean filter(List<Pair<ResourceLocation, ResourceLocation>> list, Object e) {
        Pair<ResourceLocation, ResourceLocation> pair = (Pair<ResourceLocation, ResourceLocation>) e;

        boolean apply = true;
        ResourceLocation group = pair.left();
        String id = pair.right().toString();

        if (id.contains("/")) {
            ResourceLocation itemId = ResourceLocation.tryParse(id.split(":")[0] + ":" + id.split("/")[1]);

            if (tACZ_addon$selectedAttachmentPropIndex != 0) {
                String propKey = tACZ_addon$AttachmentProp.get(tACZ_addon$selectedAttachmentPropIndex);
                Component selectedOption = Component.translatable(propKey);
                Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
                if (data != null) {
                    HashMap<String, String> AttachmentData = (HashMap<String, String>) data;
                    if (AttachmentData.get(itemId.toString()) == null) apply = false;
                    else if (!AttachmentData.get(itemId.toString()).contains(selectedOption.getString())) {
                        apply = false;
                    }
                }
            }
        }

        if (apply) list.add(pair);
        return true;
    }

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    private void tACZ_addon$restoreAttachmentFilterBeforeClassify(CallbackInfo ci) {
        if (this.tACZ_addon$browseStateRestored) {
            return;
        }

        ResourceLocation tableId = this.menu.getBlockId();

        BetterGunSmithTable.getBrowseState(tableId).ifPresent(state -> {
            int maxIndex = Math.max(0, this.tACZ_addon$AttachmentProp.size() - 1);
            this.tACZ_addon$selectedAttachmentPropIndex =
                    this.tACZ_addon$clamp(state.attachmentPropIndex(), 0, maxIndex);
        });
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
    private void tACZ_addon$restoreBrowseStateAfterClassify(CallbackInfo ci) {
        if (this.tACZ_addon$browseStateRestored) {
            return;
        }

        this.tACZ_addon$browseStateRestored = true;

        ResourceLocation tableId = this.menu.getBlockId();

        BetterGunSmithTable.getBrowseState(tableId)
                .ifPresent(this::tACZ_addon$applyBrowseState);
    }

    @Unique
    private void tACZ_addon$applyBrowseState(BetterGunSmithTable.BrowseState state) {
        int maxPropIndex = Math.max(0, this.tACZ_addon$AttachmentProp.size() - 1);
        this.tACZ_addon$selectedAttachmentPropIndex =
                this.tACZ_addon$clamp(state.attachmentPropIndex(), 0, maxPropIndex);

        ResourceLocation savedType = state.selectedType();

        if (savedType != null && this.recipes.containsKey(savedType)) {
            this.selectedType = savedType;
            this.selectedRecipeList = this.recipes.get(savedType);
        }

        this.typePage = this.tACZ_addon$typePageFor(this.selectedType, state.typePage());

        if (this.selectedRecipeList == null || this.selectedRecipeList.isEmpty()) {
            this.indexPage = 0;
            this.selectedRecipe = null;
            return;
        }

        int maxRecipePage = (this.selectedRecipeList.size() - 1) / 6;

        ResourceLocation savedRecipeId = state.selectedRecipeId();
        int recipeIndex = savedRecipeId == null ? -1 : this.selectedRecipeList.indexOf(savedRecipeId);

        if (recipeIndex >= 0) {
            this.indexPage = recipeIndex / 6;
            this.selectedRecipe = this.getSelectedRecipe(savedRecipeId);
        } else {
            this.indexPage = this.tACZ_addon$clamp(state.indexPage(), 0, maxRecipePage);

            int fallbackIndex = Math.min(this.indexPage * 6, this.selectedRecipeList.size() - 1);
            this.selectedRecipe = this.getSelectedRecipe(this.selectedRecipeList.get(fallbackIndex));
        }

        if (this.selectedRecipe == null) {
            this.indexPage = 0;
            this.selectedRecipe = this.getSelectedRecipe(this.selectedRecipeList.get(0));
        }

        if (this.selectedRecipe != null) {
            this.getPlayerIngredientCount(this.selectedRecipe);
        }
    }

    @Unique
    private int tACZ_addon$typePageFor(ResourceLocation selectedType, int fallbackTypePage) {
        int maxTypePage = this.recipeKeys.isEmpty() ? 0 : (this.recipeKeys.size() - 1) / 7;

        if (selectedType == null) {
            return this.tACZ_addon$clamp(fallbackTypePage, 0, maxTypePage);
        }

        int index = 0;
        for (ResourceLocation type : this.recipeKeys.keySet()) {
            if (selectedType.equals(type)) {
                return index / 7;
            }
            index++;
        }

        return this.tACZ_addon$clamp(fallbackTypePage, 0, maxTypePage);
    }

    @Unique
    private void tACZ_addon$saveBrowseState() {
        ResourceLocation tableId = this.menu.getBlockId();
        if (tableId == null) {
            return;
        }

        ResourceLocation selectedRecipeId =
                this.selectedRecipe == null ? null : this.selectedRecipe.getId();

        int propIndex = this.tACZ_addon$dropdown == null
                ? this.tACZ_addon$selectedAttachmentPropIndex
                : this.tACZ_addon$dropdown.getSelected();

        BetterGunSmithTable.saveBrowseState(
                tableId,
                this.selectedType,
                selectedRecipeId,
                this.typePage,
                this.indexPage,
                propIndex
        );
    }

    @Unique
    private int tACZ_addon$clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void onScreenChanged(CallbackInfo ci) {
        this.tACZ_addon$requestNearbyContainerSnapshotIfNeeded();

        if (tACZ_addon$dropdown != null) {
            tACZ_addon$selectedAttachmentPropIndex = tACZ_addon$dropdown.getSelected();
        }

        tACZ_addon$dropdown = new DropDown(leftPos - 64, topPos - 20, 64);

        for (String prop : tACZ_addon$AttachmentProp) {
            String text = Component.translatable(prop).getString().replace("+ ", "");
            tACZ_addon$dropdown.addOption(Component.translatable(text));
        }

        int maxIndex = Math.max(0, tACZ_addon$AttachmentProp.size() - 1);
        tACZ_addon$selectedAttachmentPropIndex =
                tACZ_addon$clamp(tACZ_addon$selectedAttachmentPropIndex, 0, maxIndex);

        tACZ_addon$dropdown.setSelected(tACZ_addon$selectedAttachmentPropIndex);
        tACZ_addon$dropdown.action = this::refreshRecipes;

        this.addRenderableWidget(tACZ_addon$dropdown);

        this.tACZ_addon$saveBrowseState();
    }

    private void refreshRecipes(int index) {
        tACZ_addon$selectedAttachmentPropIndex = index;
        this.tACZ_addon$saveBrowseState();
        this.updateIngredientCount();
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

        tACZ_addon$AttachmentProp.add("gui.taczaddon.gun_smith_table.default_prop");
        AttachmentPropertyManager.getModifiers().forEach((s, iAttachmentModifier) -> {
            String prop = Component.translatable("tooltip.tacz.attachment." + s + ".increase").getString();
            if (s.equals("ignite")) {
                tACZ_addon$AttachmentProp.add("tooltip.tacz.attachment.ignite.block");
                tACZ_addon$AttachmentProp.add("tooltip.tacz.attachment.ignite.entity");
                return;
            }
            if (s.equals("weight_modifier") || s.equals("recoil")) return;
            if (prop.contains("tooltip")) tACZ_addon$AttachmentProp.add("tooltip.tacz.attachment." + s);
            else tACZ_addon$AttachmentProp.add("tooltip.tacz.attachment." + s + ".increase");
        });
    }

    @Inject(method = "lambda$addCraftButton$5", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void onCrafted(Button b, CallbackInfo ci) {
        if (Config.enableGunSmithTableCraftToast()) ItemIconToast.create(
                "已制作",
                this.selectedRecipe.getOutput().getHoverName().getString() + " x " + this.selectedRecipe.getOutput().getCount(),
                this.selectedRecipe.getOutput());
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 0), index = 1, remap = true)
    private Component renderPageInfo(Component p_282131_) {
        if (this.selectedType == null || this.selectedRecipeList == null || this.selectedRecipeList.isEmpty()) {
            return Component.literal(String.format(
                    Component.translatable("gui.taczaddon.gun_smith_table.page_index").getString(),
                    "-",
                    0,
                    0
            ));
        }

        String type = Component.translatable(String.format("tacz.type.%s.name", this.selectedType.getPath())).getString();
        int maxPage = Math.max(1, (int) Math.ceil((double) this.selectedRecipeList.size() / 6.0D));
        String title = String.format(
                Component.translatable("gui.taczaddon.gun_smith_table.page_index").getString(),
                type,
                this.indexPage + 1,
                maxPage
        );
        return Component.literal(title);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = true)
    private void tACZ_addon$guardMouseScrolledWhenNoRecipes(
            double mouseX,
            double mouseY,
            double delta,
            CallbackInfoReturnable<Boolean> cir
    ) {
        boolean insideRecipeList =
                mouseX > (double) (this.leftPos + 143)
                        && mouseX < (double) (this.leftPos + 143 + 94)
                        && mouseY > (double) (this.topPos + 66)
                        && mouseY < (double) (this.topPos + 66 + 85);

        if (!insideRecipeList) {
            return;
        }

        if (this.selectedRecipeList == null || this.selectedRecipeList.isEmpty()) {
            this.indexPage = 0;
            this.selectedRecipe = null;
            cir.setReturnValue(true);
        }
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
    private int tACZ_addon$mouseX = 0;

    @Unique
    private int tACZ_addon$mouseY = 0;

    @Unique
    private HashMap<String, Boolean> tACZ_addon$hover = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), remap = true)
    private void storedMousePos(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.tACZ_addon$mouseX = mouseX;
        this.tACZ_addon$mouseY = mouseY;
    }

    @Redirect(
            method = "renderIngredient",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
            ),
            remap = true
    )
    private void renderBetterIngredient(GuiGraphics graphics, ItemStack itemStack, int x, int y) {
        graphics.renderItem(itemStack, x, y);

        String hoverKey = x + ":" + y;

        if (tACZ_addon$isHovering(x, y)) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    itemStack,
                    this.tACZ_addon$mouseX,
                    this.tACZ_addon$mouseY
            );

            DataStorage.set("GunSmithTableJEI", itemStack);
            this.tACZ_addon$hover.put(hoverKey, true);
        } else {
            this.tACZ_addon$hover.put(hoverKey, false);

            boolean anyHovering = this.tACZ_addon$hover.values()
                    .stream()
                    .anyMatch(Boolean::booleanValue);

            if (!anyHovering) {
                DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
            }
        }
    }

    @Unique
    private boolean tACZ_addon$isHovering(int x, int y) {
        int mouseX = this.tACZ_addon$mouseX;
        int mouseY = this.tACZ_addon$mouseY;

        return mouseX >= x && mouseX <= x + 16
                && mouseY >= y && mouseY <= y + 16;
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
    @Inject(method = "lambda$addCraftButton$5", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
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
