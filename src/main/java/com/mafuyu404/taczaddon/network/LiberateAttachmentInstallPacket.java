package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.common.LiberateAttachmentService;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S liberated install request. The server resolves the attachment ID from
 * its own current TaCZ data instead of trusting a client virtual slot index.
 */
public final class LiberateAttachmentInstallPacket {
    private final int gunSlotIndex;
    private final ResourceLocation attachmentId;

    public LiberateAttachmentInstallPacket(
            int gunSlotIndex,
            ResourceLocation attachmentId
    ) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentId = attachmentId;
    }

    public static void encode(
            LiberateAttachmentInstallPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(message.gunSlotIndex);
        buffer.writeResourceLocation(message.attachmentId);
    }

    public static LiberateAttachmentInstallPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new LiberateAttachmentInstallPacket(
                buffer.readInt(),
                buffer.readResourceLocation()
        );
    }

    public static void handle(
            LiberateAttachmentInstallPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(
                message,
                context.getSender()
        ));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(
            LiberateAttachmentInstallPacket message,
            @Nullable ServerPlayer player
    ) {
        if (player == null) {
            return;
        }
        if (!LiberateAttachmentService.isEnabled(player)) {
            reject(player);
            return;
        }

        ItemStack gunStack =
                LiberateAttachmentService.getCurrentRealGun(
                        player,
                        message.gunSlotIndex
                );
        if (gunStack == null) {
            reject(player);
            return;
        }

        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.hasAttachmentLock(gunStack)) {
            reject(player);
            return;
        }

        VirtualInventory virtualInventory =
                LiberateAttachmentService.createLiberatedInventory(
                        player.getInventory()
                );
        Optional<LiberateAttachmentService.Candidate> resolved =
                LiberateAttachmentService.findCandidate(
                        virtualInventory,
                        message.attachmentId
                );
        if (resolved.isEmpty()) {
            reject(player);
            return;
        }

        ItemStack candidate = resolved.get().stack().copy();
        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(candidate);
        if (attachment == null) {
            reject(player);
            return;
        }

        ResourceLocation actualId =
                attachment.getAttachmentId(candidate);
        AttachmentType actualType = attachment.getType(candidate);
        if (!message.attachmentId.equals(actualId)
                || actualType == null
                || actualType == AttachmentType.NONE
                || actualType != resolved.get().type()
                || !gun.allowAttachment(gunStack, candidate)) {
            reject(player);
            return;
        }

        ItemStack replaced = gun.getAttachment(gunStack, actualType);
        gun.installAttachment(gunStack, candidate);
        virtualInventory.add(replaced);
        AttachmentPropertyManager.postChangeEvent(player, gunStack);

        if (actualType == AttachmentType.EXTENDED_MAG) {
            gun.dropAllAmmo(player, gunStack);
        }

        player.inventoryMenu.broadcastChanges();
        LiberateAttachmentService.refreshRefitScreen(player);
    }

    private static void reject(ServerPlayer player) {
        LiberateAttachmentService.refreshRefitScreen(player);
    }
}
