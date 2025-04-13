package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;

@Mixin(value = GunHudOverlay.class, remap = false)
public class GunHudOverlayMixin {
    @ModifyArg(method = "handleCacheCount", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/gui/overlay/GunHudOverlay;handleInventoryAmmo(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Inventory;)V"))
    private static Inventory useBackpackAmmo(Inventory inventory) {
        if (DataStorage.get("backpackData") == null) {
            SophisticatedBackpacksCompat.syncAllBackpack(inventory.player);
            DataStorage.set("backpackData", true);
        }
        ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(inventory.player);
        backpack.addAll(inventory.items);
        VirtualInventory virtualInventory = new VirtualInventory(backpack.size(), inventory.player);
        for (int i = 0; i < backpack.size(); i++) {
            virtualInventory.setItem(i, backpack.get(i));
        }
        return virtualInventory;
    }
}
