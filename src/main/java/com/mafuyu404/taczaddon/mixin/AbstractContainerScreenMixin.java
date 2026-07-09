package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.ItemRelationHelper;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksClientCompat;
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

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Unique
    private Slot taczaddon$relationHoveredSlot;

    @Unique
    private ItemStack taczaddon$relationHoveredStack = ItemStack.EMPTY;

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
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        if (SophisticatedBackpacksClientCompat.isStorageScreen(screen)) {
            return;
        }

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
                return;
            }
        }
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void taczaddon$renderRelationHighlight(
            GuiGraphics guiGraphics,
            Slot slot,
            CallbackInfo ci
    ) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        if (SophisticatedBackpacksClientCompat.isStorageScreen(screen)) {
            return;
        }

        if (!Config.SHOW_ITEM_RELATION.get()) {
            return;
        }

        if (slot == this.taczaddon$relationHoveredSlot) {
            return;
        }

        ItemStack hoverItem = this.taczaddon$relationHoveredStack;
        ItemStack currentItem = slot.getItem();

        if (hoverItem.isEmpty() || currentItem.isEmpty()) {
            return;
        }

        if (!ItemRelationHelper.areRelated(hoverItem, currentItem)) {
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
}
