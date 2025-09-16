package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.VirtualContainerLoader;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class VirtualContanier implements VirtualContainerLoader {
    @Unique
    private ArrayList<ItemStack> tACZ_addon$virtualContainer = new ArrayList<>();
//
    @Override
    public void tACZ_addon$setVirtualContanier(ArrayList<ItemStack> items) {
        this.tACZ_addon$virtualContainer = items;
    }
    @Override
    public ArrayList<ItemStack> tACZ_addon$getVirtualContanier() {
        return this.tACZ_addon$virtualContainer;
    }
}
