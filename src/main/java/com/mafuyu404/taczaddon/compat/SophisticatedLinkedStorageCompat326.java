package com.mafuyu404.taczaddon.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.ClientLinkedStorageBackpackContents;
import net.p3pp3rf1y.sophisticatedbackpacks.network.LinkedStorageBackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestLinkedStorageBackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;

import java.util.UUID;

final class SophisticatedLinkedStorageCompat326
        implements SophisticatedLinkedStorageCompat.Bridge {

    @Override
    public SophisticatedLinkedStorageCompat.EndpointResolution
    resolve(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return SophisticatedLinkedStorageCompat
                    .EndpointResolution
                    .notLinked();
        }

        if (LinkedStorageStackLifecycle
                .classifyEndpoint(stack)
                != LinkedStorageEndpointStackState.ENDPOINT) {
            return SophisticatedLinkedStorageCompat
                    .EndpointResolution
                    .notLinked();
        }

        LinkedStorageEndpointData endpoint =
                LinkedStorageStackData
                        .getEndpoint(stack);

        if (endpoint == null
                || endpoint.groupId() == null) {
            return SophisticatedLinkedStorageCompat
                    .EndpointResolution
                    .malformedLinked();
        }

        return SophisticatedLinkedStorageCompat
                .EndpointResolution
                .linked(
                        endpoint.groupId()
                );
    }

    @Override
    public void requestSnapshot(
            UUID groupId
    ) {
        if (groupId == null) {
            return;
        }

        /*
         * World/session bootstrap.
         *
         * Always request a complete snapshot because Sophisticated clears its
         * client linked-storage cache when leaving a world.
         */
        SBPPacketHandler.INSTANCE
                .sendToServer(
                        new RequestLinkedStorageBackpackContentsMessage(
                                groupId,
                                -1L
                        )
                );
    }

    @Override
    public void refreshSnapshot(
            UUID groupId
    ) {
        if (groupId == null) {
            return;
        }

        /*
         * Incremental coherence probe.
         *
         * If a snapshot already exists, send its revision. The Sophisticated
         * server returns no payload when its revision is unchanged.
         *
         * If no snapshot exists, -1 falls back to the same full-bootstrap
         * behavior.
         */
        long knownRevision =
                ClientLinkedStorageBackpackContents
                        .getRevision(groupId)
                        .orElse(-1L);

        SBPPacketHandler.INSTANCE
                .sendToServer(
                        new RequestLinkedStorageBackpackContentsMessage(
                                groupId,
                                knownRevision
                        )
                );
    }

    @Override
    public void sendSnapshot(
            ServerPlayer player,
            UUID groupId
    ) {
        if (player == null
                || groupId == null) {
            return;
        }

        SBPPacketHandler.INSTANCE
                .sendToClient(
                        player,
                        LinkedStorageBackpackContentsMessage
                                .createSnapshot(
                                        player.serverLevel(),
                                        groupId
                                )
                );
    }
}