package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            StringBuilder Cpos = new StringBuilder();
            StringBuilder Bpos = new StringBuilder();
            for (int x = msg.blockPos.getX() - 2; x <= msg.blockPos.getX() + 2; x++) {
                for (int y = msg.blockPos.getY() - 1; y <= msg.blockPos.getY() + 1; y++) {
                    for (int z = msg.blockPos.getZ() - 2; z <= msg.blockPos.getZ() + 2; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), blockPos);
                        Matcher matcher = Pattern.compile("(.*backpack.*){2,}").matcher(player.level().getBlockState(blockPos).getBlock().getDescriptionId());
                        if (matcher.matches()) {
                            ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromBackpackBLock(blockPos, player);
                            items.addAll(backpack);
                            Bpos.append(String.format("%s,%s,%s;", x, y, z));
                        }
                        if (!containerContent.isEmpty()) {
                            items.addAll(containerContent);
                            Cpos.append(String.format("%s,%s,%s;", x, y, z));
                        }
                    }
                }
            }
            if (player != null) {
                if (!Cpos.isEmpty()) player.getPersistentData().putString("BetterGunSmithTable.nearbyContainerPos", Cpos.toString());
                if (!Bpos.isEmpty()) player.getPersistentData().putString("BetterGunSmithTable.nearbyBackpackPos", Bpos.toString());
            }

//            ArrayList<ItemStack> inventoryBackpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(player);
//            items.addAll(inventoryBackpack);

            NetworkHandler.sendToClient(player, new ContainerReaderPacket(items));
            NetworkHandler.sendToClient(player, new ContainerReaderPacket(items));
        });
        ctx.get().setPacketHandled(true);
    }
}

