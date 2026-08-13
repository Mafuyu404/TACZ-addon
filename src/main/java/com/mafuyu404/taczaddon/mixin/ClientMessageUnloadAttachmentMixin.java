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
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * TaCZ 1.1.8-hotfix (Curse file 8141310) obtains the inventory once at bytecode
 * offset 10 and invokes Inventory.add(ItemStack) at offset 73. Redirecting that
 * exact inventory source makes the native add write to the virtual sink.
 */
@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public abstract class ClientMessageUnloadAttachmentMixin {
    @Redirect(
            method =
                    "handle(Lcom/tacz/guns/network/message/"
                            + "ClientMessageUnloadAttachment;"
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
    private static CompletableFuture taczaddon$enqueueValidatedUnload(
            NetworkEvent.Context context,
            Runnable originalWork,
            ClientMessageUnloadAttachment message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        return context.enqueueWork(() -> {
            if (!taczaddon$validateUnloadRequest(
                    context,
                    message
            )) {
                return;
            }

            originalWork.run();
        });
    }

    private static boolean taczaddon$validateUnloadRequest(
            NetworkEvent.Context context,
            ClientMessageUnloadAttachment message
    ) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return false;
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
            refresh(player);
            return false;
        }

        boolean liberated =
                LiberateAttachmentService.isEnabled(player);

        if (!liberated) {
            return true;
        }

        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.hasAttachmentLock(gunStack)) {
            refresh(player);
            return false;
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
            refresh(player);
            return false;
        }

        return true;
    }

    @Redirect(
            method =
                    "lambda$handle$0("
                            + "Lnet/minecraftforge/network/"
                            + "NetworkEvent$Context;"
                            + "Lcom/tacz/guns/network/message/"
                            + "ClientMessageUnloadAttachment;)V",
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

    private static void refresh(ServerPlayer player) {
        LiberateAttachmentService.refreshRefitScreen(player);
    }
}
