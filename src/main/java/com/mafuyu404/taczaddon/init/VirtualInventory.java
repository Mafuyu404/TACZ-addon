package com.mafuyu404.taczaddon.init;

import com.google.common.collect.ImmutableList;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.mixin.InventoryAccessor;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VirtualInventory extends Inventory {
    public  int size;
    public int playerInventorySize;
    public VirtualInventory(int size, Player player) {
        super(Objects.requireNonNull(player));
        ((InventoryAccessor) this).setItems(NonNullList.withSize(size, ItemStack.EMPTY));
        ((InventoryAccessor) this).setCompartments(ImmutableList.of(this.items, this.armor, this.offhand));
        this.playerInventorySize = player.getInventory().getContainerSize();
        this.size = size;
    }
    public VirtualInventory extend() {
        Inventory playerInventory = this.player.getInventory();
        for (int i = 0; i < this.playerInventorySize; i++) {
            this.setItem(i, playerInventory.getItem(i));
        }
        return this;
    }
    public ItemHandler getHandler() {
        return new ItemHandler(this);
    }
    public static class ItemHandler implements IItemHandler {

        private final VirtualInventory virtualInventory;

        public ItemHandler(VirtualInventory virtualInventory) {
            this.virtualInventory = virtualInventory;
        }

        @Override
        public int getSlots() {
            return virtualInventory.size;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return virtualInventory.getItem(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return null;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack itemStack = virtualInventory.getItem(slot);
            ItemStack result = itemStack.copy();
            itemStack.setCount(itemStack.getCount() - amount);
            result.setCount(amount);
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return virtualInventory.size;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }

        // 其他方法类似，按需覆盖...
    }
//    @Override
//    public void setItem(int p_35999_, @NotNull ItemStack p_36000_) {
//        ItemStack originItem = this.getItem(p_35999_);
//        NonNullList<ItemStack> nonnulllist = null;
//        for(NonNullList<ItemStack> nonnulllist1 : ((InventoryAccessor) this).getCompartments()) {
//            if (p_35999_ < nonnulllist1.size()) {
//                nonnulllist = nonnulllist1;
//                break;
//            }
//            p_35999_ -= nonnulllist1.size();
//        }
//        if (nonnulllist != null) {
//            nonnulllist.set(p_35999_, p_36000_);
//        }
//        VirtualInventoryChangeEvent.SetItemEvent event = new VirtualInventoryChangeEvent.SetItemEvent(p_36000_, this, p_35999_, originItem);
//        MinecraftForge.EVENT_BUS.post(event);
//    }
//    @Override
//    public boolean add(ItemStack p_36055_) {
//        VirtualInventoryChangeEvent.AddEvent event = new VirtualInventoryChangeEvent.AddEvent(p_36055_, this);
//        MinecraftForge.EVENT_BUS.post(event);
//        return this.add(-1, p_36055_);
//    }
}
