package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Shared loaded-only physical inventory source policy for Gunsmith crafting
 * and attachment refitting.
 *
 * <p>Resolution priority:
 *
 * <pre>
 * Sophisticated Backpack block
 *     -> existing isolated Sophisticated facade
 *
 * generic NeoForge BLOCK ItemHandler capability
 *     -> request-scoped capability cache
 *
 * vanilla Container fallback
 *     -> ContainerMaster / InvWrapper
 * </pre>
 *
 * <p>The returned handler always implements {@link IItemHandlerModifiable}.
 * For a native modifiable handler, setStackInSlot delegates directly.
 * For a generic IItemHandler, the wrapper implements rollback by restoring
 * only the missing delta into the original slot. Slots that cannot accept
 * their own current stack are treated as read-ineligible/mutation-ineligible,
 * preventing output-only machine slots from entering transactions that could
 * not safely compensate an extraction.
 */
public final class NearbyInventorySourceResolver {

    private static final String SOPHISTICATED_BACKPACKS_NAMESPACE =
            "sophisticatedbackpacks";

    private NearbyInventorySourceResolver() {
    }

    public enum SourceKind {
        CONTAINER,
        SOPHISTICATED_BACKPACK
    }

    public record Source(
            BlockPos pos,
            SourceKind kind,
            IItemHandler handler
    ) {
        public boolean isValid() {
            return handler instanceof BlockHandler block
                    && block.isValid();
        }

        public Object backendIdentity() {
            if (!(handler instanceof BlockHandler block)) {
                return handler;
            }

            return block.backendIdentity();
        }

        public void markChanged() {
            if (handler instanceof BlockHandler block) {
                block.markChanged();
            }
        }
    }

    public static boolean inRange(
            BlockPos anchor,
            BlockPos pos,
            int horizontalRadius,
            int verticalRadius
    ) {
        return Math.abs(
                (long) pos.getX() - anchor.getX()
        ) <= horizontalRadius
                && Math.abs(
                (long) pos.getZ() - anchor.getZ()
        ) <= horizontalRadius
                && Math.abs(
                (long) pos.getY() - anchor.getY()
        ) <= verticalRadius;
    }

    public static List<Source> resolve(
            ServerPlayer player,
            BlockPos anchor,
            int horizontalRadius,
            int verticalRadius
    ) {
        int radius =
                Math.max(
                        0,
                        Math.min(horizontalRadius, 16)
                );

        int vertical =
                Math.max(
                        0,
                        Math.min(verticalRadius, 1)
                );

        ServerLevel level =
                player.serverLevel();

        BlockPos origin =
                anchor.immutable();

        List<BlockPos> positions =
                orderedPositions(
                        origin,
                        radius,
                        vertical
                );

        ArrayList<Source> result =
                new ArrayList<>();

        Set<Object> seenBackends =
                Collections.newSetFromMap(
                        new IdentityHashMap<>()
                );

        for (BlockPos pos : positions) {
            /*
             * Match the 1.20.1 source policy:
             * the Gunsmith block / player position itself is not a nearby
             * external inventory source.
             */
            if (pos.equals(origin)
                    || !level.isLoaded(pos)) {
                continue;
            }

            try {
                BlockState state =
                        level.getBlockState(pos);

                BlockEntity blockEntity =
                        level.getBlockEntity(pos);

                /*
                 * Sophisticated Backpacks MUST remain behind its existing
                 * compatibility/circuit-breaker facade.
                 *
                 * Never fall through to the generic NeoForge capability for a
                 * sophisticatedbackpacks block when the facade reports it as
                 * unavailable, otherwise an ABI-broken Soph integration could
                 * accidentally be bypassed.
                 */
                boolean sophisticatedBackpack =
                        SophisticatedBackpacksCompat
                                .isBackpackBlock(
                                        level,
                                        pos
                                );

                if (sophisticatedBackpack) {
                    if (blockEntity == null
                            || blockEntity.isRemoved()) {
                        continue;
                    }

                    SophisticatedBackpacksCompat
                            .forEachBlockBackpackHandler(
                                    player,
                                    pos,
                                    handler -> {
                                        if (handler == null) {
                                            return;
                                        }

                                        BlockHandler wrapped =
                                                BlockHandler
                                                        .forSophisticated(
                                                                player,
                                                                pos,
                                                                state,
                                                                blockEntity,
                                                                handler
                                                        );

                                        addIfUsable(
                                                result,
                                                seenBackends,
                                                new Source(
                                                        pos,
                                                        SourceKind
                                                                .SOPHISTICATED_BACKPACK,
                                                        wrapped
                                                )
                                        );
                                    }
                            );

                    continue;
                }

                /*
                 * If the actual block belongs to Sophisticated Backpacks but
                 * the facade could not recognize/use it, fail closed instead
                 * of reaching the mod through generic block capabilities.
                 */
                if (isSophisticatedBackpacksBlock(state)) {
                    continue;
                }

                /*
                 * Primary generic NeoForge path.
                 *
                 * Query first so we only allocate a BlockCapabilityCache for
                 * positions that actually expose an item handler.
                 */
                IItemHandler capability =
                        level.getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                pos,
                                null
                        );

                if (capability != null) {
                    BlockHandler wrapped =
                            BlockHandler.forCapability(
                                    player,
                                    pos,
                                    state,
                                    blockEntity
                            );

                    if (wrapped != null) {
                        addIfUsable(
                                result,
                                seenBackends,
                                new Source(
                                        pos,
                                        SourceKind.CONTAINER,
                                        wrapped
                                )
                        );
                    }

                    /*
                     * A capability existed at resolution time. If it vanished
                     * before the cache could bind, fail closed for this request
                     * rather than silently switching backend type.
                     */
                    continue;
                }

                /*
                 * Conservative vanilla Container fallback.
                 */
                if (blockEntity == null
                        || blockEntity.isRemoved()) {
                    continue;
                }

                ContainerMaster
                        .getContainerHandler(
                                level,
                                pos
                        )
                        .ifPresent(handler -> {
                            BlockHandler wrapped =
                                    BlockHandler.forContainer(
                                            player,
                                            pos,
                                            state,
                                            blockEntity,
                                            handler
                                    );

                            addIfUsable(
                                    result,
                                    seenBackends,
                                    new Source(
                                            pos,
                                            SourceKind.CONTAINER,
                                            wrapped
                                    )
                            );
                        });
            } catch (RuntimeException exception) {
                com.mojang.logging.LogUtils
                        .getLogger()
                        .warn(
                                "Skipping unavailable nearby inventory source at {}",
                                pos,
                                exception
                        );
            }
        }

        return List.copyOf(result);
    }

    private static List<BlockPos> orderedPositions(
            BlockPos origin,
            int horizontalRadius,
            int verticalRadius
    ) {
        BlockPos min =
                origin.offset(
                        -horizontalRadius,
                        -verticalRadius,
                        -horizontalRadius
                );

        BlockPos max =
                origin.offset(
                        horizontalRadius,
                        verticalRadius,
                        horizontalRadius
                );

        ArrayList<BlockPos> positions =
                new ArrayList<>();

        for (BlockPos mutable :
                BlockPos.betweenClosed(min, max)) {
            positions.add(
                    mutable.immutable()
            );
        }

        /*
         * Match 1.20.1 deterministic source ordering.
         *
         * This affects both which inventory is consumed first and the order
         * external attachment candidates are displayed in.
         */
        positions.sort(
                Comparator.comparingLong(
                        BlockPos::asLong
                )
        );

        return positions;
    }

    private static void addIfUsable(
            List<Source> result,
            Set<Object> seenBackends,
            Source source
    ) {
        try {
            if (!source.isValid()
                    || source.handler().getSlots() <= 0) {
                return;
            }

            Object identity =
                    source.backendIdentity();

            if (!seenBackends.add(identity)) {
                return;
            }

            result.add(source);
        } catch (RuntimeException exception) {
            com.mojang.logging.LogUtils
                    .getLogger()
                    .warn(
                            "Skipping unusable nearby source at {}",
                            source.pos(),
                            exception
                    );
        }
    }

    private static boolean
    isSophisticatedBackpacksBlock(
            BlockState state
    ) {
        var id =
                BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        );

        return id != null
                && SOPHISTICATED_BACKPACKS_NAMESPACE
                .equals(id.getNamespace());
    }

    /**
     * Request-scoped stable facade around the selected backend.
     *
     * <p>It deliberately implements IItemHandlerModifiable even when the
     * underlying generic capability does not. setStackInSlot is only used as
     * rollback/restore by TACZAddon. Generic handlers are compensated by
     * reinserting the exact missing delta into the original slot.
     */
    private static final class BlockHandler
            implements IItemHandlerModifiable {

        private enum BackendKind {
            BLOCK_CAPABILITY,
            CONTAINER,
            SOPHISTICATED_BACKPACK
        }

        private final ServerPlayer player;
        private final BlockPos pos;
        private final BlockState expectedState;
        private final BlockEntity expectedBlockEntity;

        private final BackendKind kind;
        private final IItemHandler expectedHandler;

        private final BlockCapabilityCache<
                IItemHandler,
                Direction
                > capabilityCache;

        private BlockHandler(
                ServerPlayer player,
                BlockPos pos,
                BlockState expectedState,
                BlockEntity expectedBlockEntity,
                BackendKind kind,
                IItemHandler expectedHandler,
                BlockCapabilityCache<
                        IItemHandler,
                        Direction
                        > capabilityCache
        ) {
            this.player = player;
            this.pos = pos.immutable();
            this.expectedState = expectedState;
            this.expectedBlockEntity =
                    expectedBlockEntity;
            this.kind = kind;
            this.expectedHandler =
                    expectedHandler;
            this.capabilityCache =
                    capabilityCache;
        }

        private static BlockHandler
        forCapability(
                ServerPlayer player,
                BlockPos pos,
                BlockState expectedState,
                BlockEntity expectedBlockEntity
        ) {
            BlockCapabilityCache<
                    IItemHandler,
                    Direction
                    > cache =
                    BlockCapabilityCache.create(
                            Capabilities.ItemHandler.BLOCK,
                            player.serverLevel(),
                            pos,
                            null
                    );

            IItemHandler handler =
                    cache.getCapability();

            if (handler == null) {
                return null;
            }

            return new BlockHandler(
                    player,
                    pos,
                    expectedState,
                    expectedBlockEntity,
                    BackendKind.BLOCK_CAPABILITY,
                    handler,
                    cache
            );
        }

        private static BlockHandler
        forContainer(
                ServerPlayer player,
                BlockPos pos,
                BlockState expectedState,
                BlockEntity expectedBlockEntity,
                IItemHandler handler
        ) {
            return new BlockHandler(
                    player,
                    pos,
                    expectedState,
                    expectedBlockEntity,
                    BackendKind.CONTAINER,
                    handler,
                    null
            );
        }

        private static BlockHandler
        forSophisticated(
                ServerPlayer player,
                BlockPos pos,
                BlockState expectedState,
                BlockEntity expectedBlockEntity,
                IItemHandler handler
        ) {
            return new BlockHandler(
                    player,
                    pos,
                    expectedState,
                    expectedBlockEntity,
                    BackendKind
                            .SOPHISTICATED_BACKPACK,
                    handler,
                    null
            );
        }

        private boolean isValid() {
            if (!basePositionValid()) {
                return false;
            }

            return currentHandler() != null;
        }

        private boolean basePositionValid() {
            ServerLevel level =
                    player.serverLevel();

            if (!level.isLoaded(pos)) {
                return false;
            }

            if (!expectedState.equals(
                    level.getBlockState(pos)
            )) {
                return false;
            }

            BlockEntity currentBlockEntity =
                    level.getBlockEntity(pos);

            if (currentBlockEntity
                    != expectedBlockEntity) {
                return false;
            }

            return expectedBlockEntity == null
                    || !expectedBlockEntity.isRemoved();
        }

        private IItemHandler currentHandler() {
            if (!basePositionValid()) {
                return null;
            }

            return switch (kind) {
                case BLOCK_CAPABILITY -> {
                    if (capabilityCache == null) {
                        yield null;
                    }

                    IItemHandler current =
                            capabilityCache
                                    .getCapability();

                    /*
                     * A capability invalidation that replaces the handler
                     * invalidates this request-scoped source. A new resolve()
                     * will bind the replacement on the next operation.
                     */
                    yield current == expectedHandler
                            ? current
                            : null;
                }

                case CONTAINER ->
                        expectedHandler;

                case SOPHISTICATED_BACKPACK ->
                        findExpectedSophisticatedHandler();
            };
        }

        private IItemHandler
        findExpectedSophisticatedHandler() {
            IItemHandler[] matched =
                    new IItemHandler[1];

            SophisticatedBackpacksCompat
                    .forEachBlockBackpackHandler(
                            player,
                            pos,
                            handler -> {
                                if (handler
                                        == expectedHandler) {
                                    matched[0] =
                                            handler;
                                }
                            }
                    );

            return matched[0];
        }

        private Object backendIdentity() {
            if (kind == BackendKind.CONTAINER
                    && expectedBlockEntity != null) {
                return expectedBlockEntity;
            }

            return expectedHandler;
        }

        private <T> T access(
                Function<IItemHandler, T> action
        ) {
            IItemHandler handler =
                    currentHandler();

            if (handler == null) {
                throw new IllegalStateException(
                        "Nearby inventory source changed: "
                                + pos
                );
            }

            return action.apply(handler);
        }

        @Override
        public int getSlots() {
            return access(
                    IItemHandler::getSlots
            );
        }

        @Override
        public ItemStack getStackInSlot(
                int slot
        ) {
            return access(handler -> {
                if (slot < 0
                        || slot >= handler.getSlots()) {
                    return ItemStack.EMPTY;
                }

                ItemStack stack =
                        handler.getStackInSlot(slot);

                /*
                 * For a generic non-modifiable handler, do not expose an
                 * extraction-only/output slot to crafting/refit. Such a slot
                 * cannot be guaranteed rollback-safe.
                 */
                if (!(handler
                        instanceof IItemHandlerModifiable)
                        && !stack.isEmpty()
                        && !handler.isItemValid(
                        slot,
                        stack
                )) {
                    return ItemStack.EMPTY;
                }

                return stack;
            });
        }

        @Override
        public ItemStack extractItem(
                int slot,
                int count,
                boolean simulate
        ) {
            return access(handler -> {
                if (slot < 0
                        || slot >= handler.getSlots()
                        || count <= 0) {
                    return ItemStack.EMPTY;
                }

                ItemStack current =
                        handler.getStackInSlot(slot);

                if (!canRestoreAfterExtraction(
                        handler,
                        slot,
                        current
                )) {
                    return ItemStack.EMPTY;
                }

                return handler.extractItem(
                        slot,
                        count,
                        simulate
                );
            });
        }

        @Override
        public ItemStack insertItem(
                int slot,
                ItemStack stack,
                boolean simulate
        ) {
            return access(handler ->
                    handler.insertItem(
                            slot,
                            stack,
                            simulate
                    )
            );
        }

        @Override
        public int getSlotLimit(
                int slot
        ) {
            return access(handler ->
                    handler.getSlotLimit(slot)
            );
        }

        @Override
        public boolean isItemValid(
                int slot,
                ItemStack stack
        ) {
            return access(handler ->
                    handler.isItemValid(
                            slot,
                            stack
                    )
            );
        }

        /**
         * TACZAddon only uses this mutation for rollback/snapshot restoration.
         */
        @Override
        public void setStackInSlot(
                int slot,
                ItemStack snapshot
        ) {
            access(handler -> {
                if (slot < 0
                        || slot >= handler.getSlots()) {
                    throw new IllegalArgumentException(
                            "Invalid nearby inventory slot "
                                    + slot
                    );
                }

                if (handler
                        instanceof IItemHandlerModifiable modifiable) {
                    modifiable.setStackInSlot(
                            slot,
                            snapshot.copy()
                    );
                    return Boolean.TRUE;
                }

                restoreGenericSnapshot(
                        handler,
                        slot,
                        snapshot
                );

                return Boolean.TRUE;
            });
        }

        private static boolean
        canRestoreAfterExtraction(
                IItemHandler handler,
                int slot,
                ItemStack current
        ) {
            if (current == null
                    || current.isEmpty()) {
                return false;
            }

            if (handler
                    instanceof IItemHandlerModifiable) {
                return true;
            }

            /*
             * Generic handler rollback is compensating insertion, therefore
             * only consume a slot that accepts its own current item.
             *
             * This intentionally excludes output-only machine slots.
             */
            return handler.isItemValid(
                    slot,
                    current
            );
        }

        private static void
        restoreGenericSnapshot(
                IItemHandler handler,
                int slot,
                ItemStack snapshot
        ) {
            ItemStack current =
                    handler.getStackInSlot(slot);

            if (ItemStack.matches(
                    current,
                    snapshot
            )) {
                return;
            }

            if (snapshot.isEmpty()) {
                throw new IllegalStateException(
                        "Generic handler rollback cannot "
                                + "remove unexpected contents from slot "
                                + slot
                );
            }

            if (!current.isEmpty()
                    && !ItemStack
                    .isSameItemSameComponents(
                            current,
                            snapshot
                    )) {
                throw new IllegalStateException(
                        "Generic handler slot identity changed "
                                + "during rollback at slot "
                                + slot
                );
            }

            int currentCount =
                    current.isEmpty()
                            ? 0
                            : current.getCount();

            int missing =
                    snapshot.getCount()
                            - currentCount;

            if (missing <= 0) {
                throw new IllegalStateException(
                        "Generic handler slot contains more "
                                + "items than the rollback snapshot at slot "
                                + slot
                );
            }

            if (!handler.isItemValid(
                    slot,
                    snapshot
            )) {
                throw new IllegalStateException(
                        "Generic handler no longer accepts its "
                                + "rollback item at slot "
                                + slot
                );
            }

            ItemStack restoring =
                    snapshot.copyWithCount(
                            missing
                    );

            ItemStack remainder;

            try {
                remainder =
                        handler.insertItem(
                                slot,
                                restoring,
                                false
                        );
            } catch (RuntimeException exception) {
                /*
                 * Some handlers may mutate and then throw. Verify the final
                 * state before declaring rollback failure.
                 */
                if (ItemStack.matches(
                        handler.getStackInSlot(slot),
                        snapshot
                )) {
                    return;
                }

                throw new IllegalStateException(
                        "Generic handler threw while restoring "
                                + "slot "
                                + slot,
                        exception
                );
            }

            if (!remainder.isEmpty()
                    || !ItemStack.matches(
                    handler.getStackInSlot(slot),
                    snapshot
            )) {
                throw new IllegalStateException(
                        "Generic handler could not exactly "
                                + "restore slot "
                                + slot
                );
            }
        }

        private void markChanged() {
            if (!basePositionValid()) {
                return;
            }

            ServerLevel level =
                    player.serverLevel();

            if (expectedBlockEntity != null) {
                expectedBlockEntity.setChanged();
            }

            BlockState state =
                    level.getBlockState(pos);

            level.sendBlockUpdated(
                    pos,
                    state,
                    state,
                    3
            );
        }
    }
}