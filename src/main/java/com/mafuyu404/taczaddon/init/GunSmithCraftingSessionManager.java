package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GunSmithCraftingSessionManager {
    static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;

    private static final Map<UUID, GunSmithCraftingSession> SESSIONS =
            new ConcurrentHashMap<>();

    private GunSmithCraftingSessionManager() {
    }

    public static GunSmithCraftingSession createSession(
            ServerPlayer player,
            int containerId,
            BlockPos tablePos,
            ResourceLocation tableBlockId
    ) {
        GunSmithCraftingSession session =
                new GunSmithCraftingSession(
                        player.getUUID(),
                        containerId,
                        player.level().dimension(),
                        tablePos.immutable(),
                        Objects.requireNonNull(
                                tableBlockId,
                                "tableBlockId"
                        )
                );

        SESSIONS.put(player.getUUID(), session);
        return session;
    }

    @Nullable
    public static GunSmithCraftingSession getSession(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static void removeSession(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public static void removeSession(
            UUID playerId,
            int containerId
    ) {
        SESSIONS.computeIfPresent(
                playerId,
                (ignored, session) ->
                        session.getContainerId() == containerId
                                ? null
                                : session
        );
    }

    public static void removeSession(
            UUID playerId,
            GunSmithCraftingSession session
    ) {
        SESSIONS.remove(playerId, session);
    }

    public static void removeAll() {
        SESSIONS.clear();
    }

    /**
     * Policy for packets that reference a gunsmith container.
     *
     * A request for an old container must never delete the player's current
     * session. A request that matches the current container may delete that
     * matching session when its server-side structural validation fails.
     */
    public static SessionRequestDecision evaluateRequest(
            @Nullable GunSmithCraftingSession session,
            int suppliedContainerId,
            boolean structurallyValid
    ) {
        if (session == null) {
            return SessionRequestDecision.REJECTED_NO_SESSION;
        }
        if (session.getContainerId() != suppliedContainerId) {
            return SessionRequestDecision.REJECTED_STALE_CONTAINER;
        }
        return structurallyValid
                ? SessionRequestDecision.ACCEPTED
                : SessionRequestDecision.REJECTED_INVALID_MATCHING_SESSION;
    }

    public enum SessionRequestDecision {
        REJECTED_NO_SESSION(false, false),
        REJECTED_STALE_CONTAINER(false, false),
        REJECTED_INVALID_MATCHING_SESSION(false, true),
        ACCEPTED(true, false);

        private final boolean accepted;
        private final boolean removeMatchingSession;

        SessionRequestDecision(
                boolean accepted,
                boolean removeMatchingSession
        ) {
            this.accepted = accepted;
            this.removeMatchingSession = removeMatchingSession;
        }

        public boolean accepted() {
            return this.accepted;
        }

        public boolean shouldRemoveMatchingSession() {
            return this.removeMatchingSession;
        }
    }

    /**
     * The complete lifetime policy for a gunsmith session. Elapsed game time
     * is deliberately absent: an idle session remains valid while its real
     * server-side menu and table relationship remains valid.
     */
    static boolean isLifecycleValid(SessionLifecycleState state) {
        return state.playerMatches()
                && state.suppliedContainerMatches()
                && state.dimensionMatches()
                && state.gunsmithMenuOpen()
                && state.activeMenuContainerMatches()
                && state.menuTableDefinitionMatches()
                && state.menuStillValid()
                && state.tableLoaded()
                && state.expectedTableEntityPresent()
                && state.tableEntityActive()
                && state.tableDefinitionMatches()
                && state.distanceSquared()
                <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    record SessionLifecycleState(
            boolean playerMatches,
            boolean suppliedContainerMatches,
            boolean dimensionMatches,
            boolean gunsmithMenuOpen,
            boolean activeMenuContainerMatches,
            boolean menuTableDefinitionMatches,
            boolean menuStillValid,
            boolean tableLoaded,
            boolean expectedTableEntityPresent,
            boolean tableEntityActive,
            boolean tableDefinitionMatches,
            double distanceSquared
    ) {
    }

    public static final class GunSmithCraftingSession {
        private final UUID playerId;
        private final int containerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos tablePos;
        private final ResourceLocation tableBlockId;

        private long lastAcceptedCraftRequestId = -1L;
        private long lastAcceptedRefreshRequestId = -1L;
        private List<CraftingSourceKey> sourceKeys = List.of();

        /*
         * Structural/addon-owned mutation revision only. It does not detect
         * arbitrary external inventory changes; every craft and refresh
         * re-resolves live server sources.
         */
        private long sourceRevision;

        GunSmithCraftingSession(
                UUID playerId,
                int containerId,
                ResourceKey<Level> dimension,
                BlockPos tablePos,
                ResourceLocation tableBlockId
        ) {
            this.playerId = playerId;
            this.containerId = containerId;
            this.dimension = dimension;
            this.tablePos = tablePos;
            this.tableBlockId = tableBlockId;
        }

        public boolean validate(
                ServerPlayer player,
                int suppliedContainerId
        ) {
            GunSmithTableMenu menu = player.containerMenu
                    instanceof GunSmithTableMenu activeMenu
                    ? activeMenu
                    : null;

            boolean tableLoaded =
                    player.level().isLoaded(this.tablePos);
            BlockEntity blockEntity = tableLoaded
                    ? player.level().getBlockEntity(this.tablePos)
                    : null;
            GunSmithTableBlockEntity table = blockEntity
                    instanceof GunSmithTableBlockEntity activeTable
                    ? activeTable
                    : null;

            double distanceSquared =
                    player.distanceToSqr(
                            this.tablePos.getX() + 0.5D,
                            this.tablePos.getY() + 0.5D,
                            this.tablePos.getZ() + 0.5D
                    );
            return isLifecycleValid(new SessionLifecycleState(
                    player.getUUID().equals(this.playerId),
                    suppliedContainerId == this.containerId,
                    player.level().dimension().equals(this.dimension),
                    menu != null,
                    menu != null
                            && menu.containerId == suppliedContainerId
                            && menu.containerId == this.containerId,
                    menu != null && Objects.equals(
                            menu.getBlockId(),
                            this.tableBlockId
                    ),
                    menu != null && menu.stillValid(player),
                    tableLoaded,
                    table != null,
                    table != null && !table.isRemoved(),
                    table != null && Objects.equals(
                            table.getId(),
                            this.tableBlockId
                    ),
                    distanceSquared
            ));
        }

        public synchronized boolean acceptCraftRequestId(
                long requestId
        ) {
            if (requestId < 0L
                    || requestId <= this.lastAcceptedCraftRequestId) {
                return false;
            }

            this.lastAcceptedCraftRequestId = requestId;
            return true;
        }

        public synchronized boolean acceptRefreshRequestId(
                long requestId
        ) {
            if (requestId < 0L
                    || requestId
                    <= this.lastAcceptedRefreshRequestId) {
                return false;
            }

            this.lastAcceptedRefreshRequestId = requestId;
            return true;
        }

        public synchronized void updateSourceKeys(
                List<CraftingSourceKey> keys
        ) {
            List<CraftingSourceKey> copy = List.copyOf(keys);
            if (!this.sourceKeys.equals(copy)) {
                this.sourceKeys = copy;
                // Structural source-set change, not a content fingerprint.
                this.sourceRevision++;
            }
        }

        public synchronized void markSourcesChanged() {
            // Addon-owned craft mutation marker, not an external content scan.
            this.sourceRevision++;
        }

        public UUID getPlayerId() {
            return this.playerId;
        }

        public int getContainerId() {
            return this.containerId;
        }

        public ResourceKey<Level> getDimension() {
            return this.dimension;
        }

        public BlockPos getTablePos() {
            return this.tablePos;
        }

        public ResourceLocation getTableBlockId() {
            return this.tableBlockId;
        }

        public synchronized List<CraftingSourceKey> getSourceKeys() {
            return this.sourceKeys;
        }

        public synchronized long getSourceRevision() {
            return this.sourceRevision;
        }
    }
}
