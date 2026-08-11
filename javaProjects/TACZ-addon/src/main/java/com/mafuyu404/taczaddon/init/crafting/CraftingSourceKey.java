package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Identifies a real inventory source without holding mutable references.
 *
 * <p>Used for deduplication and for re-resolving sources during transaction commit.</p>
 */
public sealed interface CraftingSourceKey
        permits CraftingSourceKey.PlayerInventory,
                CraftingSourceKey.BlockEntity {

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

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
