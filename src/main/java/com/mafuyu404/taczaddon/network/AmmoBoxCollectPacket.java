package com.mafuyu404.taczaddon.network;

import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AmmoBoxCollectPacket {
    private final int index;

    public AmmoBoxCollectPacket(int index) {
        this.index = index;
    }

    public static void encode(AmmoBoxCollectPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.index);
    }

    public static AmmoBoxCollectPacket decode(FriendlyByteBuf buffer) {
        return new AmmoBoxCollectPacket(buffer.readInt());
    }

    public static void handle(AmmoBoxCollectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack ammoBox = player.inventoryMenu.getSlot(msg.index).getItem();
            AmmoBoxItem ammoBoxItem = (AmmoBoxItem) ammoBox.getItem();
            player.inventoryMenu.slots.forEach(slot -> {
                if (slot.hasItem()) ammoBoxItem.overrideStackedOnOther(ammoBox, slot, ClickAction.SECONDARY, player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
