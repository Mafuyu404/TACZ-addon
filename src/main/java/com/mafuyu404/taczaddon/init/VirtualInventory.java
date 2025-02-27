package com.mafuyu404.taczaddon.init;

import com.google.common.collect.ImmutableList;
import com.mafuyu404.taczaddon.client.ClientDataStorage;
import com.mafuyu404.taczaddon.mixin.InventoryAccessor;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class VirtualInventory extends Inventory {
    public int playerInventorySize;
    public VirtualInventory(int size, Player player) {
        super(Objects.requireNonNull(player));
        ((InventoryAccessor) this).setItems(NonNullList.withSize(size, ItemStack.EMPTY));
        ((InventoryAccessor) this).setCompartments(ImmutableList.of(this.items, this.armor, this.offhand));
        this.playerInventorySize = player.getInventory().getContainerSize();
    }
    @Override
    public void setItem(int p_35999_, @NotNull ItemStack p_36000_) {
        ItemStack originItem = this.getItem(p_35999_);
        NonNullList<ItemStack> nonnulllist = null;
        for(NonNullList<ItemStack> nonnulllist1 : ((InventoryAccessor) this).getCompartments()) {
            if (p_35999_ < nonnulllist1.size()) {
                nonnulllist = nonnulllist1;
                break;
            }
            p_35999_ -= nonnulllist1.size();
        }
        if (nonnulllist != null) {
            nonnulllist.set(p_35999_, p_36000_);
        }
        VirtualInventoryChangeEvent.SetItemEvent event = new VirtualInventoryChangeEvent.SetItemEvent(p_36000_, this, p_35999_, originItem);
        MinecraftForge.EVENT_BUS.post(event);
    }
    @Override
    public boolean add(ItemStack p_36055_) {
        VirtualInventoryChangeEvent.AddEvent event = new VirtualInventoryChangeEvent.AddEvent(p_36055_, this);
        MinecraftForge.EVENT_BUS.post(event);
        return this.add(-1, p_36055_);
    }
}
