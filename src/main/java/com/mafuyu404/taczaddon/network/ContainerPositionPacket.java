package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualContainerLoader;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ContainerPositionPacket {
    private final BlockPos blockPos;

    public ContainerPositionPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(ContainerPositionPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.blockPos);
    }

    public static ContainerPositionPacket decode(FriendlyByteBuf buffer) {
        return new ContainerPositionPacket(buffer.readBlockPos());
    }

    public static void handle(ContainerPositionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            ArrayList<ItemStack> items = new ArrayList<>();
            StringBuilder pos = new StringBuilder();
            for (int x = msg.blockPos.getX() - 2; x <= msg.blockPos.getX() + 2; x++) {
                for (int y = msg.blockPos.getY() - 1; y <= msg.blockPos.getY() + 1; y++) {
                    for (int z = msg.blockPos.getZ() - 2; z <= msg.blockPos.getZ() + 2; z++) {
                        ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), new BlockPos(x, y, z));
                        if (!containerContent.isEmpty()) {
                            items.addAll(containerContent);
                            pos.append(String.format("%s,%s,%s;", x, y, z));
                        }
                    }
                }
            }
            player.getPersistentData().putString("BetterGunSmithTable.nearbyContainerPos", pos.toString());
            NetworkHandler.sendToClient(player, new ContainerReaderPacket(items));
        });
        ctx.get().setPacketHandled(true);
    }
}

