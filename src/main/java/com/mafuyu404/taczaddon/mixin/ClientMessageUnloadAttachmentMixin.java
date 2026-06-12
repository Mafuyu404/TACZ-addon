package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.GunSmithingManager;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public class ClientMessageUnloadAttachmentMixin {
    @ModifyVariable(method = "lambda$handle$2", at = @At("STORE"), ordinal = 0)
    private static Inventory taczaddon$useVirtualAttachmentInventory(Inventory inventory) {
        if (LiberateAttachment.isLiberated(inventory.player)) return LiberateAttachment.useVirtualInventory(inventory);

        List<String> attachmentItems = GunSmithingManager.getResult(inventory.getSelected());
        if (attachmentItems.isEmpty()) {
            return inventory;
        } else {
            return LiberateAttachment.useVirtualInventory(inventory);
        }
    }
}
