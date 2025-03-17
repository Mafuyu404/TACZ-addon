package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.init.VirtualContainerLoader;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ContainerReaderPacket {
    private final ArrayList<ItemStack> items;
    private final int size;

    public ContainerReaderPacket(ArrayList<ItemStack> items) {
        this.items = items;
        this.size = items.size();
    }

    public static void encode(ContainerReaderPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.size);
        msg.items.forEach(itemStack -> buffer.writeItemStack(itemStack, true));
    }

    public static ContainerReaderPacket decode(FriendlyByteBuf buffer) {
        ArrayList<ItemStack> items = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readItem());
        }
        return new ContainerReaderPacket(items);
    }

    public static void handle(ContainerReaderPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof GunSmithTableScreen screen) {
                ((VirtualContainerLoader) Minecraft.getInstance().screen).tACZ_addon$setVirtualContanier(msg.items);
                screen.updateIngredientCount();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
