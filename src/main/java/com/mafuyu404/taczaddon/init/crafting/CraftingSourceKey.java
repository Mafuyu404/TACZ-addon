package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.UUID;

public sealed interface CraftingSourceKey
        permits CraftingSourceKey.PlayerInventory,
                CraftingSourceKey.BlockEntity,
                CraftingSourceKey.BackpackPlaced,
                CraftingSourceKey.BackpackEquipped,
                CraftingSourceKey.BackpackCarried {

    String type();

    record PlayerInventory(UUID playerId) implements CraftingSourceKey {
        @Override public String type() { return "player"; }
    }

    record BlockEntity(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) implements CraftingSourceKey {
        @Override public String type() { return "block"; }
    }

    record BackpackPlaced(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) implements CraftingSourceKey {
        @Override public String type() { return "backpack_placed"; }
    }

    record BackpackEquipped(
            UUID playerId,
            String identifier
    ) implements CraftingSourceKey {
        @Override public String type() { return "backpack_equipped"; }
    }

    record BackpackCarried(
            UUID playerId,
            int inventorySlot
    ) implements CraftingSourceKey {
        @Override public String type() { return "backpack_carried"; }
    }
}