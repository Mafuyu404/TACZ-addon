package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.tacz.guns.client.gui.components.refit.InventoryAttachmentSlot;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InventoryAttachmentSlot.class, remap = false)
public interface InventoryAttachmentSlotAccess {
    @Accessor("inventory")
    Inventory taczaddon$getInventory();
}
