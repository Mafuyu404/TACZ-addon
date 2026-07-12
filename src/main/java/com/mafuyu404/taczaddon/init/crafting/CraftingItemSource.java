package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface CraftingItemSource {
    CraftingSourceKey key();
    int slotCount();
    ItemStack getStackInSlot(int slot);
    ItemStack extractItem(int slot, int amount, boolean simulate);
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    boolean isValid(ServerPlayer player);
    default boolean hasUsableBackend() { return slotCount() > 0; }
    void markChanged();
    void synchronize(ServerPlayer player);
}
