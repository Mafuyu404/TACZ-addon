package com.mafuyu404.taczaddon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CommonMessagePacket {
    private final ItemStack backpack;
    private final int slot;
    private final ItemStack item;

    public CommonMessagePacket(ItemStack backpack, int slot, ItemStack item) {
        this.backpack = backpack;
        this.slot = slot;
        this.item = item;
    }

    public CommonMessagePacket(FriendlyByteBuf buffer) {
        this.backpack = buffer.readItem();
        this.slot = buffer.readInt();
        this.item = buffer.readItem();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(this.backpack);
        buffer.writeInt(this.slot);
        buffer.writeItem(this.item);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Packet is currently unregistered; keep handler inert if it is reintroduced.
        });
        context.setPacketHandled(true);
    }
}
