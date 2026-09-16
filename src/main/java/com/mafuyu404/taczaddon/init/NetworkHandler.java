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
     * 2.9 added the attachment detail gamerule state.
     *
     * 2.10 appends the server-owned fast swap policy to the feature config
     * payload. The wire format changed, so the protocol version changed with
     * it: an older peer is rejected by Forge's channel negotiation instead of
     * mis-decoding a shorter payload. All existing packet ID assignments are
     * unchanged.
     *
     * 2.11 adds the recipe id to the gunsmith source refresh request and the
     * authorization/truncation/aggregate fields to the source snapshot. Both
     * formats changed, so the protocol changed again while every packet ID
     * stayed in place.
     */
    private static final String PROTOCOL = "2.11";

    private static final int ID_PRIMITIVE_RESERVED = 0;
    private static final int ID_SWITCH_GUN = 1;
    private static final int ID_AMMO_BOX_COLLECT = 2;
    private static final int ID_SERVER_FEATURE_CONFIG = 3;
    private static final int ID_GUNSMITH_SOURCE_REFRESH = 4;
    private static final int ID_GUNSMITH_SOURCE_SNAPSHOT = 5;
    private static final int ID_GUNSMITH_CRAFT_REQUEST = 6;
    private static final int ID_GUNSMITH_CRAFT_RESULT = 7;
    private static final int ID_LIBERATE_ATTACHMENT_STATE = 8;
    private static final int ID_LIBERATE_ATTACHMENT_INSTALL = 9;
    private static final int ID_REFIT_SOURCE_REFRESH = 10;
    private static final int ID_REFIT_SOURCE_SNAPSHOT = 11;
    private static final int ID_REFIT_EXTERNAL_INSTALL = 12;
    private static final int ID_ATTACHMENT_DETAIL_RULE_STATE = 13;

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

        CHANNEL.registerMessage(
                ID_LIBERATE_ATTACHMENT_STATE,
                LiberateAttachmentStatePacket.class,
                LiberateAttachmentStatePacket::encode,
                LiberateAttachmentStatePacket::decode,
                LiberateAttachmentStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                ID_LIBERATE_ATTACHMENT_INSTALL,
                LiberateAttachmentInstallPacket.class,
                LiberateAttachmentInstallPacket::encode,
                LiberateAttachmentInstallPacket::decode,
                LiberateAttachmentInstallPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_REFIT_SOURCE_REFRESH,
                RefitSourceRefreshRequestPacket.class,
                RefitSourceRefreshRequestPacket::encode,
                RefitSourceRefreshRequestPacket::decode,
                RefitSourceRefreshRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_REFIT_SOURCE_SNAPSHOT,
                RefitSourceSnapshotPacket.class,
                RefitSourceSnapshotPacket::encode,
                RefitSourceSnapshotPacket::decode,
                RefitSourceSnapshotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                ID_REFIT_EXTERNAL_INSTALL,
                RefitExternalAttachmentInstallPacket.class,
                RefitExternalAttachmentInstallPacket::encode,
                RefitExternalAttachmentInstallPacket::decode,
                RefitExternalAttachmentInstallPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                ID_ATTACHMENT_DETAIL_RULE_STATE, AttachmentDetailRuleStatePacket.class,
                AttachmentDetailRuleStatePacket::encode, AttachmentDetailRuleStatePacket::decode,
                AttachmentDetailRuleStatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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

    public static void sendAttachmentDetailRuleState(ServerPlayer player) {
        sendAttachmentDetailRuleState(player, player.level().getGameRules()
                .getBoolean(RuleRegistry.SHOW_ATTACHMENT_DETAIL));
    }

    public static void sendAttachmentDetailRuleState(ServerPlayer player, boolean enabled) {
        sendToClient(player, new AttachmentDetailRuleStatePacket(enabled));
    }

    public static void sendLiberateAttachmentState(
            ServerPlayer player
    ) {
        sendLiberateAttachmentState(
                player,
                player.level()
                        .getGameRules()
                        .getBoolean(RuleRegistry.LIBERATE_ATTACHMENT)
        );
    }

    public static void sendLiberateAttachmentState(
            ServerPlayer player,
            boolean enabled
    ) {
        sendToClient(
                player,
                new LiberateAttachmentStatePacket(enabled)
        );
    }
}
