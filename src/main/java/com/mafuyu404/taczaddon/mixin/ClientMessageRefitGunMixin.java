package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public abstract class ClientMessageRefitGunMixin {
    @ModifyVariable(method = "lambda$handle$0", at = @At("STORE"), ordinal = 0)
    private static Inventory modifyInventory(Inventory inventory) {
        return LiberateAttachment.useVirtualInventory(inventory);
    }

    @Inject(method = "lambda$handle$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;installAttachment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void iii(NetworkEvent.Context context, ClientMessageRefitGun message, CallbackInfo ci, ServerPlayer player, Inventory inventory, ItemStack attachmentItem, ItemStack gunItem, IGun iGun, ItemStack oldAttachmentItem) {
//        ItemStack _gunItem = player.getMainHandItem();
//        IGun _iGun = IGun.getIGunOrNull(_gunItem);
//        _iGun.installAttachment(_gunItem, attachmentItem);
//        System.out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n");
//        player.getInventory().add(new ItemStack(Items.APPLE));
    }
}
