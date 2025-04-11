package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.AmmoBoxCollectPacket;
import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InventoryScreen.class)
public class InventoryScreenMixin extends Screen {
    protected InventoryScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void betterAmmoBox(Slot slot, int index, int key, ClickType p_98868_, CallbackInfo ci) {
        if (!hasShiftDown() || key != 1) return;
        if (!(slot.getItem().getItem() instanceof AmmoBoxItem)) return;
        NetworkHandler.CHANNEL.sendToServer(new AmmoBoxCollectPacket(index));
        ci.cancel();
    }
}
