package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = GunRefitScreen.class, remap = false)
public class GunRefitScreenMixin {
    @ModifyVariable(method = "addInventoryAttachmentButtons", at = @At("STORE"), ordinal = 0)
    private Inventory modifyInventory(Inventory inventory) {
        return LiberateAttachment.useVirtualInventory(inventory);
    }
}
