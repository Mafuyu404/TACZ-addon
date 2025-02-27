package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.network.BackpackItemsPacket;
import com.mafuyu404.taczaddon.network.CommonMessagePacket;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("taczaddon", "sync_data"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    // 注册数据包
    public static void register() {
        int packetId = 0;
        CHANNEL.registerMessage(packetId++, PrimitivePacket.class, PrimitivePacket::encode, PrimitivePacket::decode, PrimitivePacket::handle);
        CHANNEL.registerMessage(packetId++, BackpackItemsPacket.class, BackpackItemsPacket::encode, BackpackItemsPacket::decode, BackpackItemsPacket::handle);
        CHANNEL.registerMessage(
                packetId++,
                CommonMessagePacket.class,
                CommonMessagePacket::encode,
                CommonMessagePacket::new,
                CommonMessagePacket::handle
        );
    }

    // 发送数据包到客户端
    public static void sendToClient(ServerPlayer player, Object packet) {
//        PrimitivePacket packet = new PrimitivePacket(key, value);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}