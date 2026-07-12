package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Source backed by a player's inventory.
 *
 * <p>Extractions go through the real player inventory so mutations
 * are immediately visible and network-synchronized by the vanilla mechanism.</p>
 */
public class PlayerInventorySource implements CraftingItemSource {

    private final UUID playerId;
    private final Inventory inventory;

    public PlayerInventorySource(ServerPlayer player) {
        this.playerId = player.getUUID();
        this.inventory = player.getInventory();
    }

    @Override
    public CraftingSourceKey key() {
        return new CraftingSourceKey.PlayerInventory(playerId);
    }

    @Override
    public int slotCount() {
        return this.inventory.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= slotCount()) return ItemStack.EMPTY;
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount() || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = this.inventory.getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, current.getCount());
        ItemStack result = current.copyWithCount(toExtract);
        if (!simulate) {
            current.shrink(toExtract);
            if (current.isEmpty()) {
                this.inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        return result;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= slotCount()) return stack.copy();
        ItemStack current = this.inventory.getItem(slot);
        if (current.isEmpty()) {
            if (!simulate) this.inventory.setItem(slot, stack.copy());
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameTags(current, stack)) {
            int space = current.getMaxStackSize() - current.getCount();
            int toInsert = Math.min(space, stack.getCount());
            if (toInsert <= 0) return stack.copy();
            if (!simulate) current.grow(toInsert);
            ItemStack remainder = stack.copyWithCount(stack.getCount() - toInsert);
            return remainder;
        }
        return stack.copy();
    }

    @Override
    public boolean isValid(ServerPlayer player) {
        return player.getUUID().equals(this.playerId);
    }

    @Override
    public void markChanged() {
        // Vanilla inventory handles change tracking automatically
    }

    @Override
    public void synchronize(ServerPlayer player) {
        // Vanilla handles inventory sync
    }
}
