package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.init.DataStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BackpackItemsPacket {
    private final ArrayList<ItemStack> backpack;
    private final String contentsUuid;

    public BackpackItemsPacket(String contentsUuid, ArrayList<ItemStack> backpack) {
        this.contentsUuid = contentsUuid;
        this.backpack = backpack;
    }

    public static void encode(BackpackItemsPacket msg, FriendlyByteBuf buffer) {
        int size = msg.backpack.size();
        buffer.ensureWritable(size * 2);
        buffer.writeInt(size);
        msg.backpack.forEach(buffer::writeItem);
        buffer.writeUtf(msg.contentsUuid);
    }

    public static BackpackItemsPacket decode(FriendlyByteBuf buffer) {
        ArrayList<ItemStack> backpack = new ArrayList<>();
        long size = buffer.readInt();
        for (int i = 0; i < size; i++) backpack.add(buffer.readItem());
        return new BackpackItemsPacket(buffer.readUtf(), backpack);
    }

    public static void handle(BackpackItemsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DataStorage.setBackpack(msg.contentsUuid, msg.backpack);
        });
        ctx.get().setPacketHandled(true);
    }
}