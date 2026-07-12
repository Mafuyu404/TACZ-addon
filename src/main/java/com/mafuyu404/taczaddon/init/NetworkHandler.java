package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;
import java.util.Optional;

public final class NetworkHandler {
    /*
     * 2.4 removes the obsolete primitive gamerule packet while preserving
     * explicit wire IDs for all active messages.
     */
    private static final String PROTOCOL = "2.4";

    private static final int ID_PRIMITIVE_RESERVED = 0;
    private static final int ID_SWITCH_GUN = 1;
    private static final int ID_AMMO_BOX_COLLECT = 2;
    private static final int ID_SERVER_FEATURE_CONFIG = 3;
    private static final int ID_GUNSMITH_SOURCE_REFRESH = 4;
    private static final int ID_GUNSMITH_SOURCE_SNAPSHOT = 5;
    private static final int ID_GUNSMITH_CRAFT_REQUEST = 6;
    private static final int ID_GUNSMITH_CRAFT_RESULT = 7;

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    Objects.requireNonNull(
                            ResourceLocation.tryBuild(
                                    "taczaddon",
                                    "sync_data"
                            )
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private NetworkHandler() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                ID_SWITCH_GUN,
                SwitchGunPacket.class,
                SwitchGunPacket::encode,
                SwitchGunPacket::decode,
                SwitchGunPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_AMMO_BOX_COLLECT,
                AmmoBoxCollectPacket.class,
                AmmoBoxCollectPacket::encode,
                AmmoBoxCollectPacket::decode,
                AmmoBoxCollectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_SERVER_FEATURE_CONFIG,
                ServerFeatureConfigSyncPacket.class,
                ServerFeatureConfigSyncPacket::encode,
                ServerFeatureConfigSyncPacket::decode,
                ServerFeatureConfigSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                ID_GUNSMITH_SOURCE_REFRESH,
                GunSmithSourceRefreshRequestPacket.class,
                GunSmithSourceRefreshRequestPacket::encode,
                GunSmithSourceRefreshRequestPacket::decode,
                GunSmithSourceRefreshRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_GUNSMITH_SOURCE_SNAPSHOT,
                GunSmithSourceSnapshotPacket.class,
                GunSmithSourceSnapshotPacket::encode,
                GunSmithSourceSnapshotPacket::decode,
                GunSmithSourceSnapshotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                ID_GUNSMITH_CRAFT_REQUEST,
                GunSmithCraftRequestPacket.class,
                GunSmithCraftRequestPacket::encode,
                GunSmithCraftRequestPacket::decode,
                GunSmithCraftRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_GUNSMITH_CRAFT_RESULT,
                GunSmithCraftResultPacket.class,
                GunSmithCraftResultPacket::encode,
                GunSmithCraftResultPacket::decode,
                GunSmithCraftResultPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToClient(
            ServerPlayer player,
            Object packet
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                packet
        );
    }

    public static void sendServerConfig(ServerPlayer player) {
        sendToClient(
                player,
                ServerFeatureConfigSyncPacket.fromServerConfig()
        );
    }
}
