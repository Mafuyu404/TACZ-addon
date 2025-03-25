package com.mafuyu404.taczaddon.network;

import com.google.gson.Gson;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwitchGunPacket {
    private final int slot;
    private static final Gson gson = new Gson();

    public SwitchGunPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(SwitchGunPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.slot);
    }

    public static SwitchGunPacket decode(FriendlyByteBuf buffer) {
        return new SwitchGunPacket(buffer.readInt());
    }

    public static void handle(SwitchGunPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Inventory inventory = player.getInventory();
            ItemStack target = inventory.getItem(msg.slot).copy();
            inventory.setItem(msg.slot, player.getMainHandItem());
            inventory.setItem(inventory.selected, target);
//            NetworkHandler.sendToClient(player, );
        });
        ctx.get().setPacketHandled(true);
    }
}
