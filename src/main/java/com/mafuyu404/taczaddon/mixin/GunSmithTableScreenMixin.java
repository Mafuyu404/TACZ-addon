package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.init.*;
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
    private boolean taczaddon$containerSnapshotRequested;

    @Unique
    @Nullable
    private BlockPos taczaddon$lastRequestedContainerPos;

    @Unique
    private void taczaddon$requestNearbyContainerSnapshotIfNeeded() {
        if (!Config.enableGunSmithTableContainerReader()) {
            return;
        }

        Object rawPos = DataStorage.get("BetterGunSmithTable.interactBlockPos");
        if (!(rawPos instanceof BlockPos blockPos)) {
            return;
        }

        if (!blockPos.equals(this.taczaddon$lastRequestedContainerPos)) {
            this.taczaddon$lastRequestedContainerPos = blockPos;
            this.taczaddon$containerSnapshotRequested = false;
        }

        if (this.taczaddon$containerSnapshotRequested) {
            return;
        }

        this.taczaddon$containerSnapshotRequested = true;
        NetworkHandler.CHANNEL.sendToServer(new ContainerPositionPacket(blockPos));
    }

    @Unique
    private ArrayList<String> taczaddon$attachmentProp;

    @Unique
    private int taczaddon$selectedAttachmentPropIndex;

    @Unique
    private DropDown taczaddon$dropdown;

    @Unique
    private boolean taczaddon$browseStateRestored;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void taczaddon$initFields(CallbackInfo ci) {
        // Mixin merges instance field initializers into the target constructor;
        // keeping allocations here avoids Apply Initialisers rejecting them.
        this.taczaddon$attachmentProp = new ArrayList<>();
        this.taczaddon$selectedAttachmentPropIndex = 0;
        this.taczaddon$dropdown = null;
        this.taczaddon$browseStateRestored = false;
        this.taczaddon$containerSnapshotRequested = false;
        this.taczaddon$lastRequestedContainerPos = null;
        this.taczaddon$hover = new HashMap<>();
        this.taczaddon$massCount = 0;
        this.taczaddon$mouseX = 0;
        this.taczaddon$mouseY = 0;
        this.taczaddon$loadAttachmentProperties();
    }

    @Redirect(method = "classifyRecipes", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean taczaddon$filterAttachmentProperty(List<Pair<ResourceLocation, ResourceLocation>> list, Object e) {
        if (!(e instanceof Pair<?, ?> rawPair)
                || !(rawPair.left() instanceof ResourceLocation group)
                || !(rawPair.right() instanceof ResourceLocation recipeId)) {
            return false;
        }

        Pair<ResourceLocation, ResourceLocation> pair = Pair.of(group, recipeId);

        boolean apply = true;
        String id = recipeId.toString();

        if (id.contains("/")) {
            ResourceLocation itemId = ResourceLocation.tryParse(id.split(":")[0] + ":" + id.split("/")[1]);

            if (itemId == null) {
                apply = false;
            } else if (taczaddon$selectedAttachmentPropIndex > 0
                    && taczaddon$selectedAttachmentPropIndex < taczaddon$attachmentProp.size()) {
                String propKey = taczaddon$attachmentProp.get(taczaddon$selectedAttachmentPropIndex);
                Component selectedOption = Component.translatable(propKey);
                Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
                if (data instanceof Map<?, ?> attachmentData) {
                    Object storedText = attachmentData.get(itemId.toString());
                    if (!(storedText instanceof String text)) apply = false;
                    else if (!text.contains(selectedOption.getString())) {
                        apply = false;
                    }
                }
            }
        }

        return apply && list.add(pair);
    }

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    private void taczaddon$restoreAttachmentFilterBeforeClassify(CallbackInfo ci) {
        if (this.taczaddon$browseStateRestored) {
            return;
        }

        ResourceLocation tableId = this.menu.getBlockId();

        BetterGunSmithTable.getBrowseState(tableId).ifPresent(state -> {
            int maxIndex = Math.max(0, this.taczaddon$attachmentProp.size() - 1);
            this.taczaddon$selectedAttachmentPropIndex =
                    this.taczaddon$clamp(state.attachmentPropIndex(), 0, maxIndex);
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
    private void taczaddon$restoreBrowseStateAfterClassify(CallbackInfo ci) {
        if (this.taczaddon$browseStateRestored) {
            return;
        }

        this.taczaddon$browseStateRestored = true;

        ResourceLocation tableId = this.menu.getBlockId();

        BetterGunSmithTable.getBrowseState(tableId)
                .ifPresent(this::taczaddon$applyBrowseState);
    }

    @Unique
    private void taczaddon$applyBrowseState(BetterGunSmithTable.BrowseState state) {
        int maxPropIndex = Math.max(0, this.taczaddon$attachmentProp.size() - 1);
        this.taczaddon$selectedAttachmentPropIndex =
                this.taczaddon$clamp(state.attachmentPropIndex(), 0, maxPropIndex);

        ResourceLocation savedType = state.selectedType();

        if (savedType != null && this.recipes.containsKey(savedType)) {
            this.selectedType = savedType;
            this.selectedRecipeList = this.recipes.get(savedType);
        }

        this.typePage = this.taczaddon$typePageFor(this.selectedType, state.typePage());

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
            this.indexPage = this.taczaddon$clamp(state.indexPage(), 0, maxRecipePage);

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
    private int taczaddon$typePageFor(ResourceLocation selectedType, int fallbackTypePage) {
        int maxTypePage = this.recipeKeys.isEmpty() ? 0 : (this.recipeKeys.size() - 1) / 7;

        if (selectedType == null) {
            return this.taczaddon$clamp(fallbackTypePage, 0, maxTypePage);
        }

        int index = 0;
        for (ResourceLocation type : this.recipeKeys.keySet()) {
            if (selectedType.equals(type)) {
                return index / 7;
            }
            index++;
        }

        return this.taczaddon$clamp(fallbackTypePage, 0, maxTypePage);
    }

    @Unique
    private void taczaddon$saveBrowseState() {
        ResourceLocation tableId = this.menu.getBlockId();
        if (tableId == null) {
            return;
        }

        ResourceLocation selectedRecipeId =
                this.selectedRecipe == null ? null : this.selectedRecipe.getId();

        int propIndex = this.taczaddon$dropdown == null
                ? this.taczaddon$selectedAttachmentPropIndex
                : this.taczaddon$dropdown.getSelected();

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
    private int taczaddon$clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void taczaddon$onScreenChanged(CallbackInfo ci) {
        this.taczaddon$requestNearbyContainerSnapshotIfNeeded();

        if (taczaddon$dropdown != null) {
            taczaddon$selectedAttachmentPropIndex = taczaddon$dropdown.getSelected();
        }

        taczaddon$dropdown = new DropDown(leftPos - 64, topPos - 20, 64);

        for (String prop : taczaddon$attachmentProp) {
            String text = Component.translatable(prop).getString().replace("+ ", "");
            taczaddon$dropdown.addOption(Component.translatable(text));
        }

        int maxIndex = Math.max(0, taczaddon$attachmentProp.size() - 1);
        taczaddon$selectedAttachmentPropIndex =
                taczaddon$clamp(taczaddon$selectedAttachmentPropIndex, 0, maxIndex);

        taczaddon$dropdown.setSelected(taczaddon$selectedAttachmentPropIndex);
        taczaddon$dropdown.action = this::taczaddon$refreshRecipes;

        this.addRenderableWidget(taczaddon$dropdown);

        this.taczaddon$saveBrowseState();
    }

    @Unique
    private void taczaddon$refreshRecipes(int index) {
        taczaddon$selectedAttachmentPropIndex = index;
        this.taczaddon$saveBrowseState();
        this.updateIngredientCount();
    }

    @Unique
    private void taczaddon$loadAttachmentProperties() {
        HashMap<String, String> StoredAttachmentData = new HashMap<>();
        TimelessAPI.getAllClientAttachmentIndex().forEach(entry -> {
            StringBuilder data = new StringBuilder();
            entry.getValue().getData().getModifier().forEach((s, jsonProperty) -> {
                jsonProperty.getComponents().forEach(component -> data.append(component.getString()));
            });
            StoredAttachmentData.put(entry.getKey().toString(), data.toString());
        });
        if (DataStorage.get("BetterGunSmithTable.storedAttachmentData") == null) DataStorage.set("BetterGunSmithTable.storedAttachmentData", StoredAttachmentData);

        taczaddon$attachmentProp.add("gui.taczaddon.gun_smith_table.default_prop");
        AttachmentPropertyManager.getModifiers().forEach((s, iAttachmentModifier) -> {
            String prop = Component.translatable("tooltip.tacz.attachment." + s + ".increase").getString();
            if (s.equals("ignite")) {
                taczaddon$attachmentProp.add("tooltip.tacz.attachment.ignite.block");
                taczaddon$attachmentProp.add("tooltip.tacz.attachment.ignite.entity");
                return;
            }
            if (s.equals("weight_modifier") || s.equals("recoil")) return;
            if (prop.contains("tooltip")) taczaddon$attachmentProp.add("tooltip.tacz.attachment." + s);
            else taczaddon$attachmentProp.add("tooltip.tacz.attachment." + s + ".increase");
        });
    }

    @Inject(method = "lambda$addCraftButton$5", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void taczaddon$onCrafted(Button b, CallbackInfo ci) {
        if (this.selectedRecipe == null) {
            return;
        }

        if (Config.enableGunSmithTableCraftToast()) ItemIconToast.create(
                "已制作",
                this.selectedRecipe.getOutput().getHoverName().getString() + " x " + this.selectedRecipe.getOutput().getCount(),
                this.selectedRecipe.getOutput());
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 0), index = 1, remap = true)
    private Component taczaddon$renderPageInfo(Component p_282131_) {
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
    private void taczaddon$guardMouseScrolledWhenNoRecipes(
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
    private Inventory taczaddon$modifyIngredientShow(Inventory inventory) {
        if (!Config.enableGunSmithTableContainerReader()) {
            return inventory;
        }

        Object self = this;
        if (!(self instanceof VirtualContainerLoader loader)) {
            return inventory;
        }

        ArrayList<ItemStack> items = loader.taczaddon$getVirtualContainer();
        if (items == null || items.isEmpty()) {
            return inventory;
        }

        VirtualInventory virtualInventory = new VirtualInventory(inventory.getContainerSize() + items.size(), inventory.player);
        virtualInventory.extend();
        for (int i = 0; i < items.size(); i++) {
            virtualInventory.setItem(virtualInventory.playerInventorySize + i, items.get(i));
        }
        return virtualInventory;
    }

    @Unique
    private int taczaddon$mouseX;

    @Unique
    private int taczaddon$mouseY;

    @Unique
    private HashMap<String, Boolean> taczaddon$hover;

    @Inject(method = "render", at = @At("HEAD"), remap = true)
    private void taczaddon$storedMousePos(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.taczaddon$mouseX = mouseX;
        this.taczaddon$mouseY = mouseY;
    }

    @Redirect(
            method = "renderIngredient",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
            ),
            remap = true
    )
    private void taczaddon$renderBetterIngredient(GuiGraphics graphics, ItemStack itemStack, int x, int y) {
        graphics.renderItem(itemStack, x, y);

        String hoverKey = x + ":" + y;

        if (taczaddon$isHovering(x, y)) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    itemStack,
                    this.taczaddon$mouseX,
                    this.taczaddon$mouseY
            );

            DataStorage.set("GunSmithTableJEI", itemStack);
            this.taczaddon$hover.put(hoverKey, true);
        } else {
            this.taczaddon$hover.put(hoverKey, false);

            boolean anyHovering = this.taczaddon$hover.values()
                    .stream()
                    .anyMatch(Boolean::booleanValue);

            if (!anyHovering) {
                DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
            }
        }
    }

    @Unique
    private boolean taczaddon$isHovering(int x, int y) {
        int mouseX = this.taczaddon$mouseX;
        int mouseY = this.taczaddon$mouseY;

        return mouseX >= x && mouseX <= x + 16
                && mouseY >= y && mouseY <= y + 16;
    }

    @Unique
    private int taczaddon$massCount;
    @Unique
    private Button.OnPress taczaddon$onCraft;
    @ModifyArg(method = "addCraftButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;<init>(IIIIIIILnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/gui/components/Button$OnPress;)V", ordinal = 0), remap = true)
    private Button.OnPress taczaddon$captureCraftButton(Button.OnPress p_94276_) {
        taczaddon$onCraft = p_94276_;
        return p_94276_;
    }
    @Inject(method = "lambda$addCraftButton$5", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"))
    private void taczaddon$massCraft(Button b, CallbackInfo ci) {
        if (!Screen.hasShiftDown()) return;
        taczaddon$massCount++;
        if (taczaddon$massCount >= Config.getMassCraftTime()) {
            taczaddon$massCount = 0;
            return;
        }
        if (taczaddon$onCraft == null) return;
        taczaddon$onCraft.onPress(b);
    }
}
