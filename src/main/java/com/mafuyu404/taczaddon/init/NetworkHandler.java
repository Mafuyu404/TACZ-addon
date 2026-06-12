package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.network.AmmoBoxCollectPacket;
import com.mafuyu404.taczaddon.network.ContainerPositionPacket;
import com.mafuyu404.taczaddon.network.ContainerReaderPacket;
import com.mafuyu404.taczaddon.network.RuleSyncPacket;
import com.mafuyu404.taczaddon.network.SwitchGunPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    private static final String PROTOCOL = "1.0";

    public static void register(IEventBus modBus) {
        modBus.addListener(NetworkHandler::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // The addon sends server-authoritative inventory and UI state packets, so both
        // sides must negotiate the same channel instead of treating it as optional.
        PayloadRegistrar registrar = event.registrar(TACZaddon.MODID).versioned(PROTOCOL);
        registrar.playToClient(RuleSyncPacket.TYPE, RuleSyncPacket.STREAM_CODEC, RuleSyncPacket::handle);
        registrar.playToClient(ContainerReaderPacket.TYPE, ContainerReaderPacket.STREAM_CODEC, ContainerReaderPacket::handle);
        registrar.playToServer(ContainerPositionPacket.TYPE, ContainerPositionPacket.STREAM_CODEC, ContainerPositionPacket::handle);
        registrar.playToServer(SwitchGunPacket.TYPE, SwitchGunPacket.STREAM_CODEC, SwitchGunPacket::handle);
        registrar.playToServer(AmmoBoxCollectPacket.TYPE, AmmoBoxCollectPacket.STREAM_CODEC, AmmoBoxCollectPacket::handle);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
