package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/**
 * A block inventory source with one backend selected for its lifetime.
 *
 * The handler/container object itself is resolved for every operation, but the
 * backend kind never changes during one crafting request. Planning,
 * simulation, commit, and rollback therefore use the same slot layout.
 */
public final class ContainerItemSource implements CraftingItemSource {
    private enum Backend {
        ITEM_HANDLER,
        CONTAINER,
        NONE
    }

    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final Backend backend;

    public ContainerItemSource(Level level, BlockPos pos) {
        this.dimension = level.dimension();
        this.pos = pos.immutable();
        this.backend = detectBackend(level, this.pos);
    }

    @Override
    public CraftingSourceKey key() {
        return new CraftingSourceKey.BlockEntity(
                this.dimension,
                this.pos
        );
    }

    @Override
    public int slotCount() {
        if (this.backend == Backend.ITEM_HANDLER) {
            IItemHandler handler = resolveHandler();
            return handler == null ? 0 : handler.getSlots();
        }

        if (this.backend == Backend.CONTAINER) {
            Container container = resolveContainer();
            return container == null ? 0 : container.getContainerSize();
        }

        return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0) {
            return ItemStack.EMPTY;
        }

        if (this.backend == Backend.ITEM_HANDLER) {
            IItemHandler handler = resolveHandler();
            if (handler == null || slot >= handler.getSlots()) {
                return ItemStack.EMPTY;
            }
            return handler.getStackInSlot(slot);
        }

        if (this.backend == Backend.CONTAINER) {
            Container container = resolveContainer();
            if (container == null || slot >= container.getContainerSize()) {
                return ItemStack.EMPTY;
            }
            return container.getItem(slot);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(
            int slot,
            int amount,
            boolean simulate
    ) {
        if (slot < 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        if (this.backend == Backend.ITEM_HANDLER) {
            IItemHandler handler = resolveHandler();
            if (handler == null || slot >= handler.getSlots()) {
                return ItemStack.EMPTY;
            }
            return handler.extractItem(slot, amount, simulate);
        }

        if (this.backend == Backend.CONTAINER) {
            Container container = resolveContainer();
            if (container == null || slot >= container.getContainerSize()) {
                return ItemStack.EMPTY;
            }

            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int extractedCount = Math.min(amount, current.getCount());
            if (simulate) {
                return current.copyWithCount(extractedCount);
            }

            ItemStack extracted =
                    container.removeItem(slot, extractedCount);
            container.setChanged();
            return extracted;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(
            int slot,
            ItemStack stack,
            boolean simulate
    ) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (slot < 0) {
            return stack.copy();
        }

        if (this.backend == Backend.ITEM_HANDLER) {
            IItemHandler handler = resolveHandler();
            if (handler == null || slot >= handler.getSlots()) {
                return stack.copy();
            }
            return handler.insertItem(slot, stack, simulate);
        }

        if (this.backend == Backend.CONTAINER) {
            Container container = resolveContainer();
            if (container == null || slot >= container.getContainerSize()) {
                return stack.copy();
            }

            if (!container.canPlaceItem(slot, stack)) {
                return stack.copy();
            }

            ItemStack current = container.getItem(slot);
            int slotLimit = Math.min(
                    container.getMaxStackSize(),
                    stack.getMaxStackSize()
            );

            if (current.isEmpty()) {
                int insertedCount =
                        Math.min(slotLimit, stack.getCount());
                if (!simulate) {
                    container.setItem(
                            slot,
                            stack.copyWithCount(insertedCount)
                    );
                    container.setChanged();
                }
                return remainder(stack, insertedCount);
            }

            if (!ItemStack.isSameItemSameTags(current, stack)) {
                return stack.copy();
            }

            int availableSpace =
                    Math.min(slotLimit, current.getMaxStackSize())
                            - current.getCount();
            if (availableSpace <= 0) {
                return stack.copy();
            }

            int insertedCount =
                    Math.min(availableSpace, stack.getCount());
            if (!simulate) {
                current.grow(insertedCount);
                container.setChanged();
            }
            return remainder(stack, insertedCount);
        }

        return stack.copy();
    }

    @Override
    public boolean isValid(ServerPlayer player) {
        if (!player.level().dimension().equals(this.dimension)
                || !player.level().isLoaded(this.pos)) {
            return false;
        }

        BlockEntity blockEntity =
                player.level().getBlockEntity(this.pos);
        if (blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }

        if (this.backend == Backend.ITEM_HANDLER) {
            IItemHandler handler = resolveHandler();
            return handler != null && handler.getSlots() > 0;
        }

        if (this.backend == Backend.CONTAINER) {
            Container container = resolveContainer();
            return container != null
                    && container.getContainerSize() > 0;
        }

        return false;
    }

    @Override
    public boolean hasUsableBackend() {
        return this.backend != Backend.NONE && this.slotCount() > 0;
    }

    @Override
    public void markChanged() {
        BlockEntity blockEntity = resolveBlockEntity();
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }

    @Override
    public void synchronize(ServerPlayer player) {
        Level level = resolveLevel();
        BlockEntity blockEntity =
                level == null ? null : level.getBlockEntity(this.pos);

        if (level != null && blockEntity != null) {
            blockEntity.setChanged();
            level.sendBlockUpdated(
                    this.pos,
                    blockEntity.getBlockState(),
                    blockEntity.getBlockState(),
                    3
            );
        }
    }

    private static Backend detectBackend(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return Backend.NONE;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.isRemoved()) {
            return Backend.NONE;
        }

        IItemHandler handler = blockEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElse(null);
        if (handler != null && handler.getSlots() > 0) {
            return Backend.ITEM_HANDLER;
        }

        if (blockEntity instanceof Container container
                && container.getContainerSize() > 0) {
            return Backend.CONTAINER;
        }

        return Backend.NONE;
    }

    @Nullable
    private Level resolveLevel() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getLevel(this.dimension);
    }

    @Nullable
    private BlockEntity resolveBlockEntity() {
        Level level = resolveLevel();
        if (level == null || !level.isLoaded(this.pos)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        return blockEntity == null || blockEntity.isRemoved()
                ? null
                : blockEntity;
    }

    @Nullable
    private IItemHandler resolveHandler() {
        if (this.backend != Backend.ITEM_HANDLER) {
            return null;
        }

        BlockEntity blockEntity = resolveBlockEntity();
        return blockEntity == null
                ? null
                : blockEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElse(null);
    }

    @Nullable
    private Container resolveContainer() {
        if (this.backend != Backend.CONTAINER) {
            return null;
        }

        BlockEntity blockEntity = resolveBlockEntity();
        return blockEntity instanceof Container container
                ? container
                : null;
    }

    private static ItemStack remainder(
            ItemStack original,
            int inserted
    ) {
        int remaining = original.getCount() - inserted;
        return remaining <= 0
                ? ItemStack.EMPTY
                : original.copyWithCount(remaining);
    }
}
