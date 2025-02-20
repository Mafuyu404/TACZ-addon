package com.mafuyu404.taczaddon.init;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class VirtualInventoryChangeEvent extends Event {
    private final ItemStack item;
    private final VirtualInventory virtualInventory;

    public VirtualInventoryChangeEvent(ItemStack item, VirtualInventory virtualInventory) {
        this.item = item;
        this.virtualInventory = virtualInventory;
    }

    public VirtualInventory getVirtualInventory() {
        return virtualInventory;
    }
    public ItemStack getItem() {
        return item;
    }

    public static class SetItemEvent extends VirtualInventoryChangeEvent {
        private final int slot;
        private final ItemStack originItem;

        public SetItemEvent(ItemStack item, VirtualInventory virtualInventory, int slot, ItemStack originItem) {
            super(item, virtualInventory);
            this.slot = slot;
            this.originItem = originItem;
        }

        public int getSlot() { return slot; }
        public ItemStack getOriginItem() { return originItem; }
    }

    public static class AddEvent extends VirtualInventoryChangeEvent {
        public AddEvent(ItemStack item, VirtualInventory virtualInventory) {
            super(item, virtualInventory);
        }
    }
}
