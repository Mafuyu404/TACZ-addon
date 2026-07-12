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
    private static final long SESSION_INACTIVITY_TIMEOUT_TICKS =
            20L * 60L;

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
                        ),
                        player.level().getGameTime()
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

    public static void removeAll() {
        SESSIONS.clear();
    }

    public static final class GunSmithCraftingSession {
        private final UUID playerId;
        private final int containerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos tablePos;
        private final ResourceLocation tableBlockId;

        private long lastValidatedGameTime;
        private long lastAcceptedCraftRequestId = -1L;
        private long lastAcceptedRefreshRequestId = -1L;
        private List<CraftingSourceKey> sourceKeys = List.of();
        private long sourceRevision;

        private GunSmithCraftingSession(
                UUID playerId,
                int containerId,
                ResourceKey<Level> dimension,
                BlockPos tablePos,
                ResourceLocation tableBlockId,
                long gameTime
        ) {
            this.playerId = playerId;
            this.containerId = containerId;
            this.dimension = dimension;
            this.tablePos = tablePos;
            this.tableBlockId = tableBlockId;
            this.lastValidatedGameTime = gameTime;
        }

        public boolean validate(
                ServerPlayer player,
                int suppliedContainerId
        ) {
            if (!player.getUUID().equals(this.playerId)
                    || suppliedContainerId != this.containerId
                    || !player.level().dimension().equals(this.dimension)) {
                return false;
            }

            if (!(player.containerMenu
                    instanceof GunSmithTableMenu menu)) {
                return false;
            }

            if (menu.containerId != suppliedContainerId
                    || menu.containerId != this.containerId
                    || !Objects.equals(
                    menu.getBlockId(),
                    this.tableBlockId
            )
                    || !menu.stillValid(player)) {
                return false;
            }

            long gameTime = player.level().getGameTime();
            if (gameTime - this.lastValidatedGameTime
                    > SESSION_INACTIVITY_TIMEOUT_TICKS) {
                return false;
            }

            if (!player.level().isLoaded(this.tablePos)) {
                return false;
            }

            BlockEntity blockEntity =
                    player.level().getBlockEntity(this.tablePos);
            if (!(blockEntity
                    instanceof GunSmithTableBlockEntity table)
                    || table.isRemoved()
                    || !Objects.equals(
                    table.getId(),
                    this.tableBlockId
            )) {
                return false;
            }

            double distanceSquared =
                    player.distanceToSqr(
                            this.tablePos.getX() + 0.5D,
                            this.tablePos.getY() + 0.5D,
                            this.tablePos.getZ() + 0.5D
                    );
            if (distanceSquared > 64.0D) {
                return false;
            }

            this.lastValidatedGameTime = gameTime;
            return true;
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
                this.sourceRevision++;
            }
        }

        public synchronized void markSourcesChanged() {
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
