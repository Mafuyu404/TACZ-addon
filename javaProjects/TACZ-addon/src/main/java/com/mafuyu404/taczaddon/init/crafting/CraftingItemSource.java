package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A source of items for the gunsmith table crafting pipeline.
 *
 * <p>Unlike the previous design that flattened real inventories into copied
 * {@code ItemStack} lists, each source keeps a real reference to its backing
 * inventory and performs extractions/insertions directly through that reference.</p>
 *
 * <p>All mutating methods support a {@code simulate} parameter. When true,
 * the operation is a dry-run; no real state changes.</p>
 */
public interface CraftingItemSource {

    /**
     * Stable identity for this source, used for deduplication.
     */
    CraftingSourceKey key();

    /**
     * Number of accessible slots in this source.
     */
    int slotCount();

    /**
     * Read the stack in the given slot without side effects.
     */
    ItemStack getStackInSlot(int slot);

    /**
     * Extract up to {@code amount} from the given slot.
     *
     * @param slot     the slot index
     * @param amount   the maximum amount to extract
     * @param simulate if true, do not mutate the source
     * @return the extracted stack (never null; may be empty)
     */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    /**
     * Insert {@code stack} into the given slot.
     *
     * @param slot     the slot index
     * @param stack    the stack to insert
     * @param simulate if true, do not mutate the source
     * @return the remainder that could not be inserted (empty on full success)
     */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /**
     * Whether this source is still accessible by the given player.
     */
    boolean isValid(ServerPlayer player);

    /**
     * Notify the source that it has been modified, triggering saves etc.
     */
    void markChanged();

    /**
     * Synchronize the latest state to the given player's client.
     */
    void synchronize(ServerPlayer player);
}
