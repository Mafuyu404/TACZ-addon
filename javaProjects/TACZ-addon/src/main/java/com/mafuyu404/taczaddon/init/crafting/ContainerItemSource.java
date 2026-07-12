package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

/**
 * Source backed by a block entity that is either a vanilla {@link Container}
 * or exposes a Forge {@link IItemHandler} capability.
 */
public class ContainerItemSource implements CraftingItemSource {

    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final int slotCount;
    private final IItemHandler handler;
    private final Container container;

    public ContainerItemSource(Level level, BlockPos pos) {
        this.dimension = level.dimension();
        this.pos = pos.immutable();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            this.slotCount = 0;
            this.handler = null;
            this.container = null;
            return;
        }

        IItemHandler foundHandler = be.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElse(null);
        if (foundHandler != null) {
            this.handler = foundHandler;
            this.slotCount = foundHandler.getSlots();
        } else if (be instanceof Container c) {
            this.handler = null;
            this.container = c;
            this.slotCount = c.getContainerSize();
        } else {
            this.handler = null;
            this.container = null;
            this.slotCount = 0;
        }
    }

    @Override
    public CraftingSourceKey key() {
        return new CraftingSourceKey.BlockEntity(dimension, pos);
    }

    @Override
    public int slotCount() {
        return slotCount;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= slotCount) return ItemStack.EMPTY;
        if (handler != null) return handler.getStackInSlot(slot);
        if (container != null) return container.getItem(slot);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount || amount <= 0) return ItemStack.EMPTY;
        if (handler != null) return handler.extractItem(slot, amount, simulate);
        if (container != null) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, current.getCount());
            ItemStack result = current.copyWithCount(toExtract);
            if (!simulate) {
                current.shrink(toExtract);
                container.setChanged();
            }
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= slotCount) return stack.copy();
        if (handler != null) return handler.insertItem(slot, stack, simulate);
        if (container != null) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                if (!simulate) {
                    container.setItem(slot, stack.copy());
                    container.setChanged();
                }
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameTags(current, stack)) {
                int space = current.getMaxStackSize() - current.getCount();
                int toInsert = Math.min(space, stack.getCount());
                if (toInsert <= 0) return stack.copy();
                if (!simulate) {
                    current.grow(toInsert);
                    container.setChanged();
                }
                return stack.copyWithCount(stack.getCount() - toInsert);
            }
            return stack.copy();
        }
        return stack.copy();
    }

    @Override
    public boolean isValid(ServerPlayer player) {
        Level level = player.level();
        if (!level.dimension().equals(dimension)) return false;
        if (!level.isLoaded(pos)) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        double dist = player.position().distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return dist <= 64.0;
    }

    @Override
    public void markChanged() {
        if (container != null) container.setChanged();
    }

    @Override
    public void synchronize(ServerPlayer player) {
        // Block entity updates are handled by vanilla chunk sync
    }
}
