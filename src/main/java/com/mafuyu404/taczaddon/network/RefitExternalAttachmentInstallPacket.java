package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.common.RefitExternalInstallService;
import com.mafuyu404.taczaddon.init.crafting.RefitSourceLocator;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RefitExternalAttachmentInstallPacket {
    private final RefitSourceLocator locator;
    private final ResourceLocation expectedAttachmentId;
    private final AttachmentType expectedType;
    private final int gunSlotIndex;
    private final long snapshotRequestId;

    public RefitExternalAttachmentInstallPacket(
            RefitSourceLocator locator,
            ResourceLocation expectedAttachmentId,
            AttachmentType expectedType,
            int gunSlotIndex,
            long snapshotRequestId
    ) {
        this.locator = locator;
        this.expectedAttachmentId = expectedAttachmentId;
        this.expectedType = expectedType;
        this.gunSlotIndex = gunSlotIndex;
        this.snapshotRequestId = snapshotRequestId;
    }

    public static void encode(
            RefitExternalAttachmentInstallPacket message,
            FriendlyByteBuf buffer
    ) {
        message.locator.encode(buffer);
        buffer.writeResourceLocation(message.expectedAttachmentId);
        buffer.writeEnum(message.expectedType);
        buffer.writeInt(message.gunSlotIndex);
        buffer.writeLong(message.snapshotRequestId);
    }

    public static RefitExternalAttachmentInstallPacket decode(
            FriendlyByteBuf buffer
    ) {
        RefitSourceLocator locator = RefitSourceLocator.decode(buffer);
        ResourceLocation expectedAttachmentId =
                buffer.readResourceLocation();
        AttachmentType expectedType =
                buffer.readEnum(AttachmentType.class);
        int gunSlotIndex = buffer.readInt();
        long snapshotRequestId = buffer.readLong();

        return new RefitExternalAttachmentInstallPacket(
                locator,
                expectedAttachmentId,
                expectedType,
                gunSlotIndex,
                snapshotRequestId
        );
    }

    public static void handle(
            RefitExternalAttachmentInstallPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                RefitExternalInstallService.handle(
                        message,
                        context.getSender()
                )
        );
        context.setPacketHandled(true);
    }

    public RefitSourceLocator locator() {
        return this.locator;
    }

    public ResourceLocation expectedAttachmentId() {
        return this.expectedAttachmentId;
    }

    public AttachmentType expectedType() {
        return this.expectedType;
    }

    public int gunSlotIndex() {
        return this.gunSlotIndex;
    }

    public long snapshotRequestId() {
        return this.snapshotRequestId;
    }
}
