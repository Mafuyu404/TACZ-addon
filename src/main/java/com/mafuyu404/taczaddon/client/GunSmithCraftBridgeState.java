package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.GunSmithCraftRequestPacket;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class GunSmithCraftBridgeState {
    private static final long PENDING_TIMEOUT_MS = 10_000L;

    private long nextCraftRequestId;
    private long pendingCraftRequestId = -1L;
    private long pendingSinceMs;

    public boolean acceptCraftResult(
            int containerId,
            long requestId
    ) {
        if (requestId != this.pendingCraftRequestId) {
            return false;
        }
        this.pendingCraftRequestId = -1L;
        this.pendingSinceMs = 0L;
        return true;
    }

    public void requestCraft(
            int containerId,
            @Nullable ResourceLocation recipeId,
            boolean shiftDown,
            int batchMax
    ) {
        if (recipeId == null) {
            return;
        }

        long now = Util.getMillis();
        if (this.pendingCraftRequestId >= 0L) {
            if (now - this.pendingSinceMs < PENDING_TIMEOUT_MS) {
                return;
            }
            this.pendingCraftRequestId = -1L;
        }

        int requestedCount = shiftDown
                ? Math.max(1, batchMax)
                : 1;
        long requestId = ++this.nextCraftRequestId;
        this.pendingCraftRequestId = requestId;
        this.pendingSinceMs = now;

        NetworkHandler.CHANNEL.sendToServer(
                new GunSmithCraftRequestPacket(
                        containerId,
                        requestId,
                        recipeId,
                        requestedCount
                )
        );
    }
}
