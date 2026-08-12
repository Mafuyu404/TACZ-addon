package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachmentService;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TaCZ 1.1.8-hotfix (Curse file 8141310) reads attachmentSlotIndex at the
 * first Inventory.getItem call in lambda$handle$0. Validate at HEAD, before
 * either client-controlled index can reach Inventory.
 */
@Mixin(value = ClientMessageRefitGun.class, remap = false)
public abstract class ClientMessageRefitGunMixin {
    @Inject(
            method = "lambda$handle$0",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void taczaddon$validateNativeRefitRequest(
            NetworkEvent.Context context,
            ClientMessageRefitGun message,
            CallbackInfo callback
    ) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            callback.cancel();
            return;
        }

        ClientMessageRefitGunAccess access =
                (ClientMessageRefitGunAccess) message;
        Inventory realInventory = player.getInventory();
        int attachmentSlot = access.taczaddon$getAttachmentSlotIndex();
        int gunSlot = access.taczaddon$getGunSlotIndex();
        boolean liberated = LiberateAttachmentService.isEnabled(player);

        // Liberated clients use TACZ-addon's ID packet. Never reinterpret a
        // virtual slot as a real inventory slot.
        if (liberated) {
            reject(player, callback);
            return;
        }

        Inventory inventory = LiberateAttachmentService.createInventory(
                realInventory,
                liberated
        );
        if (!LiberateAttachmentService.isValidIndex(
                inventory,
                attachmentSlot
        )) {
            reject(player, callback);
            return;
        }

        ItemStack gunStack =
                LiberateAttachmentService.getCurrentRealGun(
                        player,
                        gunSlot
                );
        if (gunStack == null) {
            reject(player, callback);
            return;
        }

        ItemStack candidate = inventory.getItem(attachmentSlot);
        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(candidate);
        IGun gun = IGun.getIGunOrNull(gunStack);
        AttachmentType claimedType =
                access.taczaddon$getAttachmentType();
        ResourceLocation attachmentId = attachment == null
                ? null
                : attachment.getAttachmentId(candidate);
        AttachmentType actualType = attachment == null
                ? null
                : attachment.getType(candidate);
        if (attachment == null
                || gun == null
                || !LiberateAttachmentService.isValidCandidate(
                attachmentId,
                attachmentId,
                claimedType,
                actualType,
                gun.hasAttachmentLock(gunStack),
                gun.allowAttachment(gunStack, candidate)
        )) {
            reject(player, callback);
        }
    }

    private static void reject(
            ServerPlayer player,
            CallbackInfo callback
    ) {
        LiberateAttachmentService.refreshRefitScreen(player);
        callback.cancel();
    }
}