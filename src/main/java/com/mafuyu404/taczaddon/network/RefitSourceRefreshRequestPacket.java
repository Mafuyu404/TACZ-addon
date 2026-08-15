package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.common.RefitSourceResolver;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class RefitSourceRefreshRequestPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final long requestId;

    public RefitSourceRefreshRequestPacket(long requestId) {
        this.requestId = requestId;
    }

    public static void encode(
            RefitSourceRefreshRequestPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeLong(message.requestId);
    }

    public static RefitSourceRefreshRequestPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new RefitSourceRefreshRequestPacket(
                buffer.readLong()
        );
    }

    public static void handle(
            RefitSourceRefreshRequestPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(
                message,
                context.getSender()
        ));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(
            RefitSourceRefreshRequestPacket message,
            @Nullable ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        try {
            NetworkHandler.sendToClient(
                    player,
                    new RefitSourceSnapshotPacket(
                            message.requestId,
                            RefitSourceResolver
                                    .resolveExternalCandidates(player)
                    )
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit source refresh failed for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            NetworkHandler.sendToClient(
                    player,
                    new RefitSourceSnapshotPacket(
                            message.requestId,
                            java.util.List.of()
                    )
            );
        }
    }
}
