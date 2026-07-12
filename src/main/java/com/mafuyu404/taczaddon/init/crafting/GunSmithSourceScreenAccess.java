package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface GunSmithSourceScreenAccess {
    boolean taczaddon$acceptSourceSnapshot(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    );

    void taczaddon$requestSourceRefresh();

    List<ItemStack> taczaddon$getExternalDisplayStacks();
}
