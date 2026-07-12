package com.mafuyu404.taczaddon.init.crafting;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Source backed by a Sophisticated Backpack handler.
 *
 * <p>Extractions and insertions go through the real backpack's
 * {@code IItemHandler} or {@code InventoryHandler}, not through copied stacks.</p>
 *
 * <p>This class is loaded only when Sophisticated Backpacks is present,
 * through the compat layer's lazy isolation.</p>
 */
public class BackpackItemSource implements CraftingItemSource {

    private final CraftingSourceKey key;
    private final BackpackHandlerBridge bridge;

    /**
     * Abstracts the actual backpack handler so this class does not
     * directly reference Sophisticated Backpacks types.
     */
    public interface BackpackHandlerBridge {
        int slotCount();
        ItemStack getStackInSlot(int slot);
        ItemStack extractItem(int slot, int amount, boolean simulate);
        ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
        boolean isValid(ServerPlayer player);
        void markChanged();
        void synchronize(ServerPlayer player);
    }

    public BackpackItemSource(CraftingSourceKey key, BackpackHandlerBridge bridge) {
        this.key = key;
        this.bridge = bridge;
    }

    @Override
    public CraftingSourceKey key() {
        return key;
    }

    @Override
    public int slotCount() {
        return bridge.slotCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return bridge.getStackInSlot(slot);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return bridge.extractItem(slot, amount, simulate);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return bridge.insertItem(slot, stack, simulate);
    }

    @Override
    public boolean isValid(ServerPlayer player) {
        return bridge.isValid(player);
    }

    @Override
    public void markChanged() {
        bridge.markChanged();
    }

    @Override
    public void synchronize(ServerPlayer player) {
        bridge.synchronize(player);
    }
}
