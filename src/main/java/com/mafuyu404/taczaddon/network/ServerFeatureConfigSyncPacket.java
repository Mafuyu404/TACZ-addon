package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.client.ClientGunSmithPacketHandler;
import com.mafuyu404.taczaddon.init.CommonConfig;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Synchronizes server-owned policy needed by client request and prediction
 * logic.
 */
public final class ServerFeatureConfigSyncPacket {
    private final int batchCraftMax;
    private final boolean enableShootWhileReloading;

    private ServerFeatureConfigSyncPacket(
            int batchCraftMax,
            boolean enableShootWhileReloading
    ) {
        this.batchCraftMax = batchCraftMax;
        this.enableShootWhileReloading =
                enableShootWhileReloading;
    }

    public static ServerFeatureConfigSyncPacket fromServerConfig() {
        return new ServerFeatureConfigSyncPacket(
                CommonConfig.getBatchCraftMax(),
                CommonConfig.enableShootWhileReloading()
        );
    }

    public static void encode(
            ServerFeatureConfigSyncPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(message.batchCraftMax);
        buffer.writeBoolean(
                message.enableShootWhileReloading
        );
    }

    public static ServerFeatureConfigSyncPacket decode(
            FriendlyByteBuf buffer
    ) {
        int batchCraftMax =
                buffer.readVarInt();

        if (batchCraftMax < CommonConfig.MIN_BATCH_CRAFT
                || batchCraftMax > CommonConfig.MAX_BATCH_CRAFT) {
            throw new DecoderException(
                    "Invalid gunsmith batch maximum: "
                            + batchCraftMax
            );
        }

        boolean enableShootWhileReloading =
                buffer.readBoolean();

        return new ServerFeatureConfigSyncPacket(
                batchCraftMax,
                enableShootWhileReloading
        );
    }

    public static void handle(
            ServerFeatureConfigSyncPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ClientGunSmithPacketHandler
                                        .handleConfigSync(message)
                )
        );

        context.setPacketHandled(true);
    }

    public int batchCraftMax() {
        return this.batchCraftMax;
    }

    public boolean enableShootWhileReloading() {
        return this.enableShootWhileReloading;
    }
}