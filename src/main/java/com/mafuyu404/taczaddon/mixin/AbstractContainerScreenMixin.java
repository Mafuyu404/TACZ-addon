package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.client.ItemRelationService;
import com.mafuyu404.taczaddon.init.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Unique
    private Slot taczaddon$relationHoveredSlot;

    @Unique
    private ItemStack taczaddon$relationHoveredStack = ItemStack.EMPTY;

    @Unique
    private ItemStack taczaddon$relationCachedHoverStack = ItemStack.EMPTY;

    @Unique
    private int taczaddon$relationCachedMenuHash;

    @Unique
    private Set<Integer> taczaddon$relatedSlotIndices = Set.of();

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void taczaddon$captureRelationHoveredSlot(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        this.taczaddon$relationHoveredSlot = null;
        this.taczaddon$relationHoveredStack = ItemStack.EMPTY;

        if (!Config.SHOW_ITEM_RELATION.get()) {
            return;
        }

        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || !slot.hasItem()) {
                continue;
            }

            if (taczaddon$isMouseOverSlot(slot, mouseX, mouseY)) {
                this.taczaddon$relationHoveredSlot = slot;
                this.taczaddon$relationHoveredStack = slot.getItem();
                this.taczaddon$refreshRelationCache(slot);
                return;
            }
        }

        this.taczaddon$relatedSlotIndices = Set.of();
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void taczaddon$renderRelationHighlight(
            GuiGraphics guiGraphics,
            Slot slot,
            CallbackInfo ci
    ) {
        if (!Config.SHOW_ITEM_RELATION.get()) {
            return;
        }

        if (slot == this.taczaddon$relationHoveredSlot) {
            return;
        }

        if (!this.taczaddon$relatedSlotIndices.contains(slot.index)) {
            return;
        }

        AbstractContainerScreen.renderSlotHighlight(
                guiGraphics,
                slot.x,
                slot.y,
                0,
                0x80FFA500
        );
    }

    @Unique
    private boolean taczaddon$isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
        return mouseX >= this.leftPos + slot.x
                && mouseX < this.leftPos + slot.x + 16
                && mouseY >= this.topPos + slot.y
                && mouseY < this.topPos + slot.y + 16;
    }

    @Unique
    private void taczaddon$refreshRelationCache(Slot hoveredSlot) {
        int menuHash = this.taczaddon$menuContentHash();
        if (ItemStack.isSameItemSameComponents(this.taczaddon$relationCachedHoverStack, this.taczaddon$relationHoveredStack)
                && this.taczaddon$relationCachedMenuHash == menuHash) {
            return;
        }

        this.taczaddon$relationCachedHoverStack = this.taczaddon$relationHoveredStack.copy();
        this.taczaddon$relationCachedMenuHash = menuHash;
        this.taczaddon$relatedSlotIndices = ItemRelationService.findRelatedSlots(
                this.menu,
                hoveredSlot,
                this.taczaddon$relationHoveredStack
        );
    }

    @Unique
    private int taczaddon$menuContentHash() {
        int hash = 1;
        for (Slot slot : this.menu.slots) {
            ItemStack stack = slot.getItem();
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getComponents().hashCode());
        }
        return hash;
    }
}
