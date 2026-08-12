package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachmentService;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TaCZ 1.1.8-hotfix (Curse file 8141310) obtains the inventory once at bytecode
 * offset 10 and invokes Inventory.add(ItemStack) at offset 73. Redirecting that
 * exact inventory source makes the native add write to the virtual sink.
 */
@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public abstract class ClientMessageUnloadAttachmentMixin {
    @Inject(
            method = "lambda$handle$0",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void taczaddon$validateUnloadRequest(
            NetworkEvent.Context context,
            ClientMessageUnloadAttachment message,
            CallbackInfo callback
    ) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            callback.cancel();
            return;
        }

        ClientMessageUnloadAttachmentAccess access =
                (ClientMessageUnloadAttachmentAccess) message;
        AttachmentType requestedType =
                access.taczaddon$getAttachmentType();
        ItemStack gunStack =
                LiberateAttachmentService.getCurrentRealGun(
                        player,
                        access.taczaddon$getGunSlotIndex()
                );
        if (gunStack == null
                || requestedType == null
                || requestedType == AttachmentType.NONE) {
            reject(player, callback);
            return;
        }

        boolean liberated =
                LiberateAttachmentService.isEnabled(player);

        if (!liberated) {
            return;
        }

        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.hasAttachmentLock(gunStack)) {
            reject(player, callback);
            return;
        }

        ItemStack installed = gun.getAttachment(
                gunStack,
                requestedType
        );
        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(installed);
        if (installed.isEmpty()
                || attachment == null
                || attachment.getType(installed) != requestedType
                || attachment.getAttachmentId(installed) == null) {
            reject(player, callback);
        }
    }

    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    remap = true
            ),
            require = 1,
            remap = false
    )
    private static Inventory taczaddon$selectUnloadInventory(
            ServerPlayer player
    ) {
        Inventory realInventory = player.getInventory();
        return LiberateAttachmentService.createInventory(
                realInventory,
                LiberateAttachmentService.isEnabled(player)
        );
    }

    private static void reject(
            ServerPlayer player,
            CallbackInfo callback
    ) {
        LiberateAttachmentService.refreshRefitScreen(player);
        callback.cancel();
    }
}