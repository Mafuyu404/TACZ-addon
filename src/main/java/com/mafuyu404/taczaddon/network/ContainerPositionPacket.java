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

public class ContainerPositionPacket implements CustomPacketPayload {
    public static final Type<ContainerPositionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TACZaddon.MODID, "container_position"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerPositionPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(ContainerPositionPacket::new, ContainerPositionPacket::blockPos);
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
        return blockPos;
    }

    public static void handle(ContainerPositionPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || msg.blockPos == null) {
                return;
            }
            if (!Config.enableGunSmithTableContainerReader()) {
                return;
            }
            if (!(player.containerMenu instanceof GunSmithTableMenu)) {
                return;
            }
            if (ServerboundPacketGuard.isRateLimited(player, TYPE.id(), COOLDOWN_TICKS)) {
                return;
            }
            if (msg.blockPos.distToCenterSqr(player.position()) > MAX_REQUEST_DISTANCE_SQR) {
                return;
            }
            if (!player.level().isLoaded(msg.blockPos)) {
                return;
            }

            List<ItemStack> items = new ArrayList<>();
            List<BlockPos> containerPositions = new ArrayList<>();
            List<BlockPos> backpackPositions = new ArrayList<>();

            for (int x = msg.blockPos.getX() - SCAN_HORIZONTAL_RADIUS; x <= msg.blockPos.getX() + SCAN_HORIZONTAL_RADIUS; x++) {
                for (int y = msg.blockPos.getY() - SCAN_VERTICAL_RADIUS; y <= msg.blockPos.getY() + SCAN_VERTICAL_RADIUS; y++) {
                    for (int z = msg.blockPos.getZ() - SCAN_HORIZONTAL_RADIUS; z <= msg.blockPos.getZ() + SCAN_HORIZONTAL_RADIUS; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        if (!player.level().isLoaded(blockPos)) {
                            continue;
                        }
                        List<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), blockPos);

                        if (SophisticatedBackpacksCompat.isBackpackBlock(player.level(), blockPos)) {
                            List<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromBackpackBLock(blockPos, player);
                            if (!backpack.isEmpty()) {
                                addCopiesWithinLimit(items, backpack);
                                backpackPositions.add(blockPos.immutable());
                            }
                        }

                        if (!containerContent.isEmpty()) {
                            addCopiesWithinLimit(items, containerContent);
                            containerPositions.add(blockPos.immutable());
                        }
                    }
                }
            }

            ContainerReaderState.setSnapshot(player, containerPositions, backpackPositions);
            NetworkHandler.sendToClient(player, new ContainerReaderPacket(items));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void addCopiesWithinLimit(List<ItemStack> target, List<ItemStack> source) {
        for (ItemStack stack : source) {
            if (target.size() >= MAX_RETURNED_STACKS) {
                return;
            }
            if (!stack.isEmpty()) {
                target.add(stack.copy());
            }
        }
    }
}
