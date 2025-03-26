package com.mafuyu404.taczaddon.init;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public interface VirtualContainerLoader {

    void tACZ_addon$setVirtualContanier(ArrayList<ItemStack> items);

    ArrayList<ItemStack> tACZ_addon$getVirtualContanier();

    void refreshRecipes(String prop, boolean refreshPage);
}
