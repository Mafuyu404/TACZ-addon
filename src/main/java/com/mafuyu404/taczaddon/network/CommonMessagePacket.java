package com.mafuyu404.taczaddon.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CommonMessagePacket {
    private final ItemStack backpack;
    private final int slot;
    private final ItemStack item;

    // 构造函数（客户端使用）
    public CommonMessagePacket(ItemStack backpack, int slot, ItemStack item) {
        this.backpack = backpack;
        this.slot = slot;
        this.item = item;
    }

    // 反序列化（服务端用）
    public CommonMessagePacket(FriendlyByteBuf buffer) {
        this.backpack = buffer.readItem();
        this.slot = buffer.readInt();
        this.item = buffer.readItem();
    }

    // 序列化（客户端用）
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(this.backpack);
        buffer.writeInt(this.slot);
        buffer.writeItem(this.item);
    }

    // 处理消息（服务端用）
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
//            // 在此处处理消息逻辑
////            System.out.println("收到客户端消息: " + this.message);
//            // 示例：向服务端玩家发送反馈
//            ServerPlayer player = context.getSender();
//            BackpackWrapper backpackWrapper = new BackpackWrapper(this.backpack);
//            backpackWrapper.getInventoryHandler().setSlotStack(this.slot, this.item);
////            backpackWrapper.getInventoryHandler().saveInventory();
//            UUID uuid = backpackWrapper.getContentsUuid().get();
////            BackpackStorage.get().setBackpackContents(uuid, this.backpack);
////            new BackpackWrapper().getInventoryHandler().setSlotStack();
//            SBPPacketHandler.INSTANCE.sendToClient(player, new BackpackContentsMessage(uuid, BackpackStorage.get().getOrCreateBackpackContents(uuid)));
//            if (context.getSender() != null) {
////                StorageContainerMenuBase
////                context.getSender().sendSystemMessage(Component.nullToEmpty(BackpackStorage.get().getOrCreateBackpackContents(uuid).toString()));
//            }
//            BackpackStorage.get().setDirty();
        });
        context.setPacketHandled(true);
    }
}
