package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public class ClientMessageUnloadAttachmentMixin {
    @ModifyVariable(method = "lambda$handle$0", at = @At("STORE"), ordinal = 0)
    private static Inventory modifyInventory(Inventory inventory) {
        return LiberateAttachment.useVirtualInventory(inventory);
    }
}
