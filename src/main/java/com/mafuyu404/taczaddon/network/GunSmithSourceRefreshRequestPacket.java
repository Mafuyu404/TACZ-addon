package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mafuyu404.taczaddon.init.GunSmithCraftingSources;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public final class GunSmithSourceRefreshRequestPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int containerId;
    private final long requestId;

    public GunSmithSourceRefreshRequestPacket(
            int containerId,
            long requestId
    ) {
        this.containerId = containerId;
        this.requestId = requestId;
    }

    public static void encode(
            GunSmithSourceRefreshRequestPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(message.containerId);
        buffer.writeLong(message.requestId);
    }

    public static GunSmithSourceRefreshRequestPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new GunSmithSourceRefreshRequestPacket(
                buffer.readInt(),
                buffer.readLong()
        );
    }

    public static void handle(
            GunSmithSourceRefreshRequestPacket message,
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
            GunSmithSourceRefreshRequestPacket message,
            @Nullable ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        GunSmithCraftingSessionManager.GunSmithCraftingSession session =
                GunSmithCraftingSessionManager.getSession(
                        player.getUUID()
                );

        boolean structurallyValid =
                session != null
                        && session.validate(
                        player,
                        message.containerId
                );
        GunSmithCraftingSessionManager.SessionRequestDecision decision =
                GunSmithCraftingSessionManager.evaluateRequest(
                        session,
                        message.containerId,
                        structurallyValid
                );

        if (decision.shouldRemoveMatchingSession()) {
            GunSmithCraftingSessionManager.removeSession(
                    player.getUUID(),
                    message.containerId
            );
        }

        if (!decision.accepted()) {

            NetworkHandler.sendToClient(
                    player,
                    new GunSmithSourceSnapshotPacket(
                            message.containerId,
                            message.requestId,
                            0L,
                            List.of()
                    )
            );
            return;
        }

        if (!session.acceptRefreshRequestId(message.requestId)) {
            return;
        }

        GunSmithCraftingSources.ResolvedSources resolved;
        try {
            resolved =
                    GunSmithCraftingSources.resolve(player, session);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Gunsmith source refresh failed for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            NetworkHandler.sendToClient(
                    player,
                    new GunSmithSourceSnapshotPacket(
                            message.containerId,
                            message.requestId,
                            session.getSourceRevision(),
                            List.of()
                    )
            );
            return;
        }

        NetworkHandler.sendToClient(
                player,
                new GunSmithSourceSnapshotPacket(
                        message.containerId,
                        message.requestId,
                        session.getSourceRevision(),
                        resolved.externalStacks()
                )
        );
    }
}
