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
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * TaCZ 1.1.8-hotfix (Curse file 8141310) enqueues packet work from the
 * real handle method. Wrap that task so all player/inventory validation
 * runs on the server scheduled thread before the original worker runs.
 */
@Mixin(value = ClientMessageRefitGun.class, remap = false)
public abstract class ClientMessageRefitGunMixin {
    @Redirect(
            method =
                    "handle(Lcom/tacz/guns/network/message/"
                            + "ClientMessageRefitGun;"
                            + "Ljava/util/function/Supplier;)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraftforge/network/NetworkEvent$Context;"
                                    + "enqueueWork(Ljava/lang/Runnable;)"
                                    + "Ljava/util/concurrent/CompletableFuture;",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static CompletableFuture taczaddon$enqueueValidatedRefit(
            NetworkEvent.Context context,
            Runnable originalWork,
            ClientMessageRefitGun message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        return context.enqueueWork(() -> {
            if (!taczaddon$validateNativeRefitRequest(
                    context,
                    message
            )) {
                return;
            }

            originalWork.run();
        });
    }

    private static boolean taczaddon$validateNativeRefitRequest(
            NetworkEvent.Context context,
            ClientMessageRefitGun message
    ) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return false;
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
            refresh(player);
            return false;
        }

        Inventory inventory = LiberateAttachmentService.createInventory(
                realInventory,
                liberated
        );
        if (!LiberateAttachmentService.isValidIndex(
                inventory,
                attachmentSlot
        )) {
            refresh(player);
            return false;
        }

        ItemStack gunStack =
                LiberateAttachmentService.getCurrentRealGun(
                        player,
                        gunSlot
                );
        if (gunStack == null) {
            refresh(player);
            return false;
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
            refresh(player);
            return false;
        }

        return true;
    }

    private static void refresh(ServerPlayer player) {
        LiberateAttachmentService.refreshRefitScreen(player);
    }
}
