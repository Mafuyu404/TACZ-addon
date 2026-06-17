package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.ContainerReaderState;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class ContainerPositionPacket
        implements CustomPacketPayload {

    public static final Type<ContainerPositionPacket> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TACZaddon.MODID,
                            "container_position"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ContainerPositionPacket
            > STREAM_CODEC =
            BlockPos.STREAM_CODEC
                    .<RegistryFriendlyByteBuf>cast()
                    .map(
                            ContainerPositionPacket::new,
                            ContainerPositionPacket::blockPos
                    );

    private static final int SCAN_HORIZONTAL_RADIUS = 2;
    private static final int SCAN_VERTICAL_RADIUS = 1;

    private static final double MAX_REQUEST_DISTANCE_SQR = 64.0D;
    private static final int MAX_RETURNED_STACKS = 216;
    private static final int COOLDOWN_TICKS = 10;

    private final BlockPos blockPos;

    public ContainerPositionPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    private BlockPos blockPos() {
        return this.blockPos;
    }

    public static void handle(
            ContainerPositionPacket message,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (message.blockPos == null) {
                return;
            }

            if (!Config.enableGunSmithTableContainerReader()) {
                return;
            }

            if (!(player.containerMenu instanceof GunSmithTableMenu)) {
                return;
            }

            if (ServerboundPacketGuard.isRateLimited(
                    player,
                    TYPE.id(),
                    COOLDOWN_TICKS
            )) {
                return;
            }

            if (message.blockPos.distToCenterSqr(player.position())
                    > MAX_REQUEST_DISTANCE_SQR) {
                return;
            }

            if (!player.level().isLoaded(message.blockPos)) {
                return;
            }

            scanAndSend(player, message.blockPos);
        });
    }

    /**
     * Performs the initial scan, stores the discovered source positions,
     * and sends their current contents to the client.
     */
    private static void scanAndSend(
            ServerPlayer player,
            BlockPos tablePos
    ) {
        List<ItemStack> items = new ArrayList<>();
        List<BlockPos> containerPositions = new ArrayList<>();
        List<BlockPos> backpackPositions = new ArrayList<>();

        for (
                int x = tablePos.getX() - SCAN_HORIZONTAL_RADIUS;
                x <= tablePos.getX() + SCAN_HORIZONTAL_RADIUS;
                x++
        ) {
            for (
                    int y = tablePos.getY() - SCAN_VERTICAL_RADIUS;
                    y <= tablePos.getY() + SCAN_VERTICAL_RADIUS;
                    y++
            ) {
                for (
                        int z = tablePos.getZ() - SCAN_HORIZONTAL_RADIUS;
                        z <= tablePos.getZ() + SCAN_HORIZONTAL_RADIUS;
                        z++
                ) {
                    BlockPos sourcePos = new BlockPos(x, y, z);

                    if (!player.level().isLoaded(sourcePos)) {
                        continue;
                    }

                    if (SophisticatedBackpacksCompat.isBackpackBlock(
                            player.level(),
                            sourcePos
                    )) {
                        List<ItemStack> backpackItems =
                                SophisticatedBackpacksCompat
                                        .getItemsFromBackpackBLock(
                                                sourcePos,
                                                player
                                        );

                        if (!backpackItems.isEmpty()) {
                            addCopiesWithinLimit(items, backpackItems);
                            backpackPositions.add(sourcePos.immutable());
                        }
                    }

                    List<ItemStack> containerItems =
                            ContainerMaster.readContainerFromPos(
                                    player.level(),
                                    sourcePos
                            );

                    if (!containerItems.isEmpty()) {
                        addCopiesWithinLimit(items, containerItems);
                        containerPositions.add(sourcePos.immutable());
                    }
                }
            }
        }

        ContainerReaderState.setSnapshot(
                player,
                containerPositions,
                backpackPositions
        );

        NetworkHandler.sendToClient(
                player,
                new ContainerReaderPacket(items)
        );
    }

    /**
     * Re-reads the source positions discovered by the initial scan.
     *
     * Call this after the server has consumed crafting ingredients.
     */
    public static void refreshStoredSnapshot(ServerPlayer player) {
        if (!Config.enableGunSmithTableContainerReader()) {
            return;
        }

        if (!(player.containerMenu instanceof GunSmithTableMenu)) {
            return;
        }

        ContainerReaderState.getSnapshot(player).ifPresent(snapshot -> {
            List<ItemStack> refreshedItems = new ArrayList<>();

            for (BlockPos sourcePos : snapshot.backpackPositions()) {
                if (refreshedItems.size() >= MAX_RETURNED_STACKS) {
                    break;
                }

                if (!player.level().isLoaded(sourcePos)) {
                    continue;
                }

                addCopiesWithinLimit(
                        refreshedItems,
                        SophisticatedBackpacksCompat
                                .getItemsFromBackpackBLock(
                                        sourcePos,
                                        player
                                )
                );
            }

            for (BlockPos sourcePos : snapshot.containerPositions()) {
                if (refreshedItems.size() >= MAX_RETURNED_STACKS) {
                    break;
                }

                if (!player.level().isLoaded(sourcePos)) {
                    continue;
                }

                addCopiesWithinLimit(
                        refreshedItems,
                        ContainerMaster.readContainerFromPos(
                                player.level(),
                                sourcePos
                        )
                );
            }

            NetworkHandler.sendToClient(
                    player,
                    new ContainerReaderPacket(refreshedItems)
            );
        });
    }

    private static void addCopiesWithinLimit(
            List<ItemStack> target,
            List<ItemStack> source
    ) {
        for (ItemStack stack : source) {
            if (target.size() >= MAX_RETURNED_STACKS) {
                return;
            }

            if (!stack.isEmpty()) {
                target.add(stack.copy());
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}