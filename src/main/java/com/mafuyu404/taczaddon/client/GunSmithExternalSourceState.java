package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import com.mafuyu404.taczaddon.network.GunSmithSourceRefreshRequestPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class GunSmithExternalSourceState {
    private static final int REFRESH_INTERVAL_TICKS = 30;
    private static final int REFRESH_TIMEOUT_TICKS = 100;

    private final RefreshSender refreshSender;

    private long nextRefreshRequestId;
    private int trackedContainerId = Integer.MIN_VALUE;
    private int ticksUntilRefresh = REFRESH_INTERVAL_TICKS;

    private long pendingRefreshRequestId = -1L;
    private long latestAcceptedRefreshRequestId = -1L;
    private boolean refreshInFlight;
    private int pendingRefreshAgeTicks;

    private boolean hasAcceptedSnapshot;
    private long latestAcceptedSourceRevision = -1L;
    private List<ItemStack> externalDisplayStacks = List.of();

    @FunctionalInterface
    public interface RefreshSender {
        void send(int containerId, long requestId);
    }

    public GunSmithExternalSourceState() {
        this((containerId, requestId) ->
                NetworkHandler.CHANNEL.sendToServer(
                        new GunSmithSourceRefreshRequestPacket(
                                containerId,
                                requestId
                        )
                )
        );
    }

    public GunSmithExternalSourceState(RefreshSender refreshSender) {
        this.refreshSender = refreshSender;
    }

    public GunSmithSourceScreenAccess.AcceptResult acceptSourceSnapshot(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    ) {
        if (containerId != this.trackedContainerId) {
            return GunSmithSourceScreenAccess.AcceptResult.REJECTED;
        }

        if (!this.refreshInFlight) {
            return GunSmithSourceScreenAccess.AcceptResult.REJECTED;
        }

        if (requestId != this.pendingRefreshRequestId) {
            return GunSmithSourceScreenAccess.AcceptResult.REJECTED;
        }

        if (requestId <= this.latestAcceptedRefreshRequestId) {
            this.clearPendingRefresh();
            return GunSmithSourceScreenAccess.AcceptResult.REJECTED;
        }

        List<ItemStack> normalizedSnapshot =
                immutableSnapshot(externalStacks);

        boolean changed = !this.hasAcceptedSnapshot
                || sourceRevision != this.latestAcceptedSourceRevision
                || !sameSnapshot(
                this.externalDisplayStacks,
                normalizedSnapshot
        );

        this.latestAcceptedRefreshRequestId = requestId;
        this.clearPendingRefresh();
        this.ticksUntilRefresh = REFRESH_INTERVAL_TICKS;

        if (!changed) {
            return GunSmithSourceScreenAccess.AcceptResult.UNCHANGED;
        }

        this.externalDisplayStacks = normalizedSnapshot;
        this.latestAcceptedSourceRevision = sourceRevision;
        this.hasAcceptedSnapshot = true;

        return GunSmithSourceScreenAccess.AcceptResult.UPDATED;
    }

    public void requestSourceRefresh(int containerId) {
        if (containerId != this.trackedContainerId) {
            this.resetForContainer(containerId);
        }

        long requestId = ++this.nextRefreshRequestId;
        this.pendingRefreshRequestId = requestId;
        this.refreshInFlight = true;
        this.pendingRefreshAgeTicks = 0;

        this.refreshSender.send(containerId, requestId);
    }

    public void tickSourceRefresh(int containerId) {
        if (containerId != this.trackedContainerId) {
            return;
        }

        if (this.refreshInFlight) {
            this.pendingRefreshAgeTicks++;
            if (this.pendingRefreshAgeTicks
                    >= REFRESH_TIMEOUT_TICKS) {
                this.clearPendingRefresh();
                this.requestSourceRefresh(containerId);
            }
            return;
        }

        if (--this.ticksUntilRefresh > 0) {
            return;
        }

        this.ticksUntilRefresh = REFRESH_INTERVAL_TICKS;
        this.requestSourceRefresh(containerId);
    }

    public void onScreenInit(int containerId) {
        if (containerId != this.trackedContainerId) {
            this.resetForContainer(containerId);
        }

        if (!this.hasAcceptedSnapshot
                && !this.refreshInFlight) {
            this.requestSourceRefresh(containerId);
        }
    }

    public List<ItemStack> getExternalDisplayStacks() {
        return this.externalDisplayStacks;
    }

    private void resetForContainer(int containerId) {
        this.clearPendingRefresh();
        this.trackedContainerId = containerId;
        this.ticksUntilRefresh = REFRESH_INTERVAL_TICKS;
        this.latestAcceptedRefreshRequestId = -1L;
        this.latestAcceptedSourceRevision = -1L;
        this.hasAcceptedSnapshot = false;
        this.externalDisplayStacks = List.of();
    }

    private void clearPendingRefresh() {
        this.pendingRefreshRequestId = -1L;
        this.refreshInFlight = false;
        this.pendingRefreshAgeTicks = 0;
    }

    private static List<ItemStack> immutableSnapshot(
            List<ItemStack> stacks
    ) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }

        ArrayList<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }

        if (copies.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(copies);
    }

    static boolean sameSnapshot(
            List<ItemStack> left,
            List<ItemStack> right
    ) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null
                || left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            if (!ItemStack.matches(
                    left.get(index),
                    right.get(index)
            )) {
                return false;
            }
        }

        return true;
    }
}
