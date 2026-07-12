package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registry.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a real inventory source without holding mutable references.
 *
 * <p>Used for deduplication and for re-resolving sources during transaction commit.</p>
 */
public sealed interface CraftingSourceKey
        permits CraftingSourceKey.PlayerInventory,
                CraftingSourceKey.BlockEntity,
                CraftingSourceKey.BackpackPlaced,
                CraftingSourceKey.BackpackEquipped,
                CraftingSourceKey.BackpackCarried {

    /** A type-tag so consumers can switch on known subtypes. */
    String type();

    /** The player's UUID. */
    record PlayerInventory(UUID playerId) implements CraftingSourceKey {
        @Override
        public String type() { return "player"; }
    }

    /** A placed block entity (vanilla container or Forge capability). */
    record BlockEntity(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) implements CraftingSourceKey {
        @Override
        public String type() { return "block"; }
    }

    /** A Sophisticated Backpack placed in the world. */
    record BackpackPlaced(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) implements CraftingSourceKey {
        @Override
        public String type() { return "backpack_placed"; }
    }

    /** An equipped Sophisticated Backpack. */
    record BackpackEquipped(
            UUID playerId,
            String identifier
    ) implements CraftingSourceKey {
        @Override
        public String type() { return "backpack_equipped"; }
    }

    /** A carried backpack item stack identified by its slot. */
    record BackpackCarried(
            UUID playerId,
            int inventorySlot
    ) implements CraftingSourceKey {
        @Override
        public String type() { return "backpack_carried"; }
    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
