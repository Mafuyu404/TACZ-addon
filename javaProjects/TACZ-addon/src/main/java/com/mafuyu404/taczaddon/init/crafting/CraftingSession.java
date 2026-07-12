package com.mafuyu404.taczaddon.init.crafting;

import com.mafuyu404.taczaddon.init.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side session tracking for gunsmith-table container access.
 *
 * <p>Replaces the old approach of storing encoded block positions in
 * {@code player.getPersistentData()}. The server owns the session and
 * resolves nearby containers authoritatively.</p>
 */
public final class CraftingSession {

    private final UUID playerId;
    private final int containerId;
    private final ResourceKey<Level> dimension;
    private final BlockPos tablePos;
    private final long createdTick;
    private long lastValidationTick;
    private final List<CraftingSourceKey> resolvedSources = new ArrayList<>();

    private static final long SESSION_TIMEOUT_TICKS = 20 * 60; // 60 seconds
    private static final Map<UUID, CraftingSession> activeSessions = new ConcurrentHashMap<>();

    private CraftingSession(
            UUID playerId,
            int containerId,
            ResourceKey<Level> dimension,
            BlockPos tablePos,
            long createdTick
    ) {
        this.playerId = playerId;
        this.containerId = containerId;
        this.dimension = dimension;
        this.tablePos = tablePos;
        this.createdTick = createdTick;
        this.lastValidationTick = createdTick;
    }

    /**
     * Create or replace a session for the given player.
     */
    public static CraftingSession create(
            ServerPlayer player,
            int containerId,
            BlockPos tablePos
    ) {
        ResourceKey<Level> dim = player.level().dimension();
        UUID playerId = player.getUUID();
        CraftingSession session = new CraftingSession(
                playerId,
                containerId,
                dim,
                tablePos,
                player.level().getGameTime()
        );
        activeSessions.put(playerId, session);
        return session;
    }

    /**
     * Get the active session for a player, or null.
     */
    @Nullable
    public static CraftingSession get(UUID playerId) {
        return activeSessions.get(playerId);
    }

    /**
     * Remove the session for a player.
     */
    public static void remove(UUID playerId) {
        activeSessions.remove(playerId);
    }

    /**
     * Validate that the session is still active and the player is still
     * eligible to interact with the table.
     */
    public boolean validate(ServerPlayer player, int containerId) {
        // Check player identity
        if (!player.getUUID().equals(this.playerId)) return false;

        // Check container ID matches
        if (this.containerId != containerId) return false;

        // Check dimension
        if (!player.level().dimension().equals(this.dimension)) return false;

        // Check session timeout
        long tick = player.level().getGameTime();
        if (tick - this.createdTick > SESSION_TIMEOUT_TICKS) return false;

        // Check table still exists
        if (!player.level().isLoaded(this.tablePos)) return false;
        if (!player.level().getBlockState(this.tablePos).is(
                player.level().getBlockState(this.tablePos).getBlock())) return false;

        // Check distance
        double dist = player.position().distanceToSqr(
                this.tablePos.getX() + 0.5,
                this.tablePos.getY() + 0.5,
                this.tablePos.getZ() + 0.5
        );
        if (dist > 64.0) return false; // 8 blocks squared

        this.lastValidationTick = tick;
        return true;
    }

    public UUID getPlayerId() { return playerId; }
    public int getContainerId() { return containerId; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public BlockPos getTablePos() { return tablePos; }

    public List<CraftingSourceKey> getResolvedSources() {
        return Collections.unmodifiableList(resolvedSources);
    }

    public void setResolvedSources(List<CraftingSourceKey> sources) {
        this.resolvedSources.clear();
        this.resolvedSources.addAll(sources);
    }

    /**
     * Scan for nearby containers around the table position.
     * The radius is server-configured and bounded.
     */
    public List<CraftingSourceKey> discoverSources(ServerPlayer player) {
        if (!CommonConfig.enableContainerReader()) {
            return Collections.emptyList();
        }

        int radius = CommonConfig.getContainerScanRadius();
        Level level = player.level();
        List<CraftingSourceKey> sources = new ArrayList<>();

        // Always include the player inventory first
        sources.add(new CraftingSourceKey.PlayerInventory(player.getUUID()));

        // Scan for nearby block entities
        BlockPos min = this.tablePos.offset(-radius, -1, -radius);
        BlockPos max = this.tablePos.offset(radius, 1, radius);
        ResourceKey<Level> dim = level.dimension();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;

            var be = level.getBlockEntity(pos);
            if (be == null) continue;

            // Vanilla Container or IItemHandler capability
            sources.add(new CraftingSourceKey.BlockEntity(dim, pos.immutable()));
        }

        this.resolvedSources.clear();
        this.resolvedSources.addAll(sources);
        return Collections.unmodifiableList(this.resolvedSources);
    }
}
