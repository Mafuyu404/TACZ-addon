package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record VirtualAttachmentRefitPacket(
        int sourceSlot,
        int gunSlot,
        ResourceLocation attachmentId
) implements CustomPacketPayload {

    /**
     * sourceSlot == -1 means that the attachment is virtual.
     * sourceSlot >= 0 means that it comes from the real player inventory.
     */
    public static final int VIRTUAL_SOURCE_SLOT = -1;

    private static final int HOTBAR_SIZE = 9;
    private static final int COOLDOWN_TICKS = 2;

    public static final Type<VirtualAttachmentRefitPacket> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TACZaddon.MODID,
                            "virtual_attachment_refit"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VirtualAttachmentRefitPacket
            > STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            VirtualAttachmentRefitPacket::sourceSlot,

            ByteBufCodecs.VAR_INT,
            VirtualAttachmentRefitPacket::gunSlot,

            ResourceLocation.STREAM_CODEC,
            VirtualAttachmentRefitPacket::attachmentId,

            VirtualAttachmentRefitPacket::new
    );

    public static void handle(
            VirtualAttachmentRefitPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (ServerboundPacketGuard.isRateLimited(
                    player,
                    TYPE.id(),
                    COOLDOWN_TICKS
            )) {
                return;
            }

            Inventory inventory = player.getInventory();

            /*
             * The refit screen modifies the currently selected main-hand gun.
             * Do not allow the client to name another arbitrary inventory slot.
             */
            if (packet.gunSlot < 0
                    || packet.gunSlot >= HOTBAR_SIZE
                    || packet.gunSlot != inventory.selected) {
                return;
            }

            ItemStack gunStack =
                    inventory.getItem(packet.gunSlot);

            IGun gun = IGun.getIGunOrNull(gunStack);

            if (gun == null || gun.hasAttachmentLock(gunStack)) {
                return;
            }

            ItemStack attachmentStack;

            if (packet.sourceSlot == VIRTUAL_SOURCE_SLOT) {
                attachmentStack =
                        getValidatedVirtualAttachment(
                                player,
                                gunStack,
                                packet.attachmentId
                        ).orElse(ItemStack.EMPTY);
            } else {
                attachmentStack =
                        getValidatedPhysicalAttachment(
                                inventory,
                                packet.sourceSlot,
                                packet.attachmentId
                        ).orElse(ItemStack.EMPTY);
            }

            if (attachmentStack.isEmpty()) {
                return;
            }

            IAttachment attachment =
                    IAttachment.getIAttachmentOrNull(
                            attachmentStack
                    );

            if (attachment == null) {
                return;
            }

            ResourceLocation actualAttachmentId =
                    attachment.getAttachmentId(
                            attachmentStack
                    );

            if (!packet.attachmentId.equals(actualAttachmentId)) {
                return;
            }

            if (!gun.allowAttachment(
                    gunStack,
                    attachmentStack
            )) {
                return;
            }

            AttachmentType actualType =
                    attachment.getType(attachmentStack);

            if (actualType == AttachmentType.NONE) {
                return;
            }

            var registryAccess = player.registryAccess();

            ItemStack oldAttachment =
                    gun.getAttachment(
                            registryAccess,
                            gunStack,
                            actualType
                    ).copy();

            gun.installAttachment(
                    registryAccess,
                    gunStack,
                    attachmentStack
            );

            if (packet.sourceSlot
                    == VIRTUAL_SOURCE_SLOT) {
                returnOldAttachment(
                        inventory,
                        oldAttachment
                );
            } else {
                inventory.setItem(
                        packet.sourceSlot,
                        oldAttachment
                );
            }

            AttachmentPropertyManager.postChangeEvent(
                    player,
                    gunStack
            );

            if (actualType
                    == AttachmentType.EXTENDED_MAG) {
                gun.dropAllAmmo(player, gunStack);
            }

            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();

            NetworkHandler.sendToClient(
                    player,
                    new RefreshRefitScreenPacket(true)
            );
        });
    }

    private static Optional<ItemStack>
    getValidatedVirtualAttachment(
            ServerPlayer player,
            ItemStack gunStack,
            ResourceLocation attachmentId
    ) {
        if (!LiberateAttachment.canUseVirtualAttachment(
                player,
                gunStack,
                attachmentId
        )) {
            return Optional.empty();
        }

        return LiberateAttachment
                .findAttachmentStack(attachmentId)
                .map(ItemStack::copy);
    }

    private static Optional<ItemStack>
    getValidatedPhysicalAttachment(
            Inventory inventory,
            int sourceSlot,
            ResourceLocation expectedId
    ) {
        if (sourceSlot < 0
                || sourceSlot
                >= inventory.getContainerSize()) {
            return Optional.empty();
        }

        if (sourceSlot == inventory.selected) {
            return Optional.empty();
        }

        ItemStack stack =
                inventory.getItem(sourceSlot);

        if (stack.isEmpty()) {
            return Optional.empty();
        }

        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(stack);

        if (attachment == null) {
            return Optional.empty();
        }

        ResourceLocation actualId =
                attachment.getAttachmentId(stack);

        if (!expectedId.equals(actualId)) {
            return Optional.empty();
        }

        return Optional.of(stack);
    }

    private static void returnOldAttachment(
            Inventory inventory,
            ItemStack oldAttachment
    ) {
        if (oldAttachment.isEmpty()) {
            return;
        }

        inventory.placeItemBackInInventory(
                oldAttachment
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}