package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.GunSmithingManager;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public class ClientMessageUnloadAttachmentMixin {
    @Unique
    private static ItemStack storedUnloadAttachment = ItemStack.EMPTY;

    @Inject(method = "lambda$handle$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void store(NetworkEvent.Context context, ClientMessageUnloadAttachment message, CallbackInfo ci, ServerPlayer player, Inventory inventory, ItemStack gunItem, IGun iGun, ItemStack attachmentItem) {
        storedUnloadAttachment = attachmentItem;
    }

    @ModifyVariable(method = "lambda$handle$0", at = @At("STORE"), ordinal = 0)
    private static Inventory modifyInventory(Inventory inventory) {
        if (LiberateAttachment.isLiberated(inventory.player)) return LiberateAttachment.useVirtualInventory(inventory);

        List<String> AttachmentItems = GunSmithingManager.getResult(inventory.getSelected());
        if (AttachmentItems.isEmpty()) {
            return inventory;
        } else {
            return LiberateAttachment.useVirtualInventory(inventory);
        }

//        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(storedUnloadAttachment);
//        if (iAttachment != null) {
//            if (AttachmentItems.contains(iAttachment.getAttachmentId(storedUnloadAttachment).toString())) {
//                return LiberateAttachment.useVirtualInventory(inventory);
//            }
//        }
    }
}
