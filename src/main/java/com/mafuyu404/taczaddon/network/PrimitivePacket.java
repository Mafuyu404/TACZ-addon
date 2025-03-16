package com.mafuyu404.taczaddon.network;

import com.google.gson.Gson;
import com.mafuyu404.taczaddon.init.DataStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PrimitivePacket {
    private final String packet;
    private static final Gson gson = new Gson();

    public static class Packet {
        public String key;
        public Object value;
        public Packet(String k, Object v) {
            this.key = k;
            this.value = v;
        }
    }

    public PrimitivePacket(String k, Object v) {
        this.packet = gson.toJson(new Packet(k, v));
    }

    public static void encode(PrimitivePacket msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.packet);
    }

    public static PrimitivePacket decode(FriendlyByteBuf buffer) {
        Packet data = gson.fromJson(buffer.readUtf(), Packet.class);
        return new PrimitivePacket(data.key, data.value);
    }

    public static void handle(PrimitivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Packet data = gson.fromJson(msg.packet, Packet.class);
            DataStorage.set(data.key, data.value);
        });
        ctx.get().setPacketHandled(true);
    }
}