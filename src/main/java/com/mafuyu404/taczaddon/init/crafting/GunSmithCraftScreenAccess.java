package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface GunSmithCraftScreenAccess {
    boolean taczaddon$acceptCraftResult(
            int containerId,
            long requestId,
            boolean success,
            int craftedExecutions,
            ItemStack outputPerCraft,
            @Nullable CraftingTransaction.CraftFailure failure
    );
}
