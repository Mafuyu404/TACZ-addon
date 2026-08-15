package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface GunSmithSourceScreenAccess {
    enum AcceptResult {
        UNCHANGED,
        UPDATED,
        REJECTED
    }

    AcceptResult taczaddon$acceptSourceSnapshot(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    );

    void taczaddon$requestSourceRefresh();

    void taczaddon$tickSourceRefresh();

    void taczaddon$onScreenInit();

    List<ItemStack> taczaddon$getExternalDisplayStacks();
}
