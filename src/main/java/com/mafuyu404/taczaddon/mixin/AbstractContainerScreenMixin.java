package com.mafuyu404.taczaddon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;

    @Shadow protected int leftPos;

    @Shadow protected int topPos;

    protected AbstractContainerScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack hoverItem = this.hoveredSlot.getItem();
            ItemStack currentItem = slot.getItem();
            if (checkRelation(hoverItem, currentItem) || checkRelation(currentItem, hoverItem)) {
                int x = slot.x;
                int y = slot.y;
                guiGraphics.fill(x, y, x + 16, y + 16, 0x80FFA500);
            }
        }
    }

    private static boolean checkRelation(ItemStack gunItem, ItemStack itemStack) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        IAmmo iAmmo = IAmmo.getIAmmoOrNull(itemStack);
        boolean isAttachment = iGun != null && iGun.allowAttachment(gunItem, itemStack);
        boolean isAmmo = iAmmo != null && iAmmo.isAmmoOfGun(gunItem, itemStack);
        return isAttachment || isAmmo;
    }
}
