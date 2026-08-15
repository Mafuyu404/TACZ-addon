package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.mafuyu404.taczaddon.init.crafting.RefitSourceLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefitSourceResolverTest {
    @Test
    void identicalStacksInDifferentSourcesStayIndependent() {
        ItemStack scope = new ItemStack(Items.IRON_INGOT);
        CraftingItemSource sourceA = fakeSource(
                new BlockPos(1, 64, 1),
                scope.copy()
        );
        CraftingItemSource sourceB = fakeSource(
                new BlockPos(2, 64, 1),
                scope.copy()
        );

        List<CraftingItemSource> sources =
                List.of(sourceA, sourceB);

        RefitSourceLocator locator = new RefitSourceLocator(
                Level.OVERWORLD,
                new BlockPos(2, 64, 1),
                0
        );
        assertTrue(RefitSourceResolver.findSource(
                sources,
                locator
        ).isPresent());
        assertSame(
                sourceB,
                RefitSourceResolver.findSource(sources, locator).get()
        );
    }

    private static CraftingItemSource fakeSource(
            BlockPos pos,
            ItemStack stack
    ) {
        return new CraftingItemSource() {
            @Override
            public CraftingSourceKey key() {
                return new CraftingSourceKey.BlockEntity(
                        Level.OVERWORLD,
                        pos
                );
            }

            @Override
            public int slotCount() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 ? stack.copy() : ItemStack.EMPTY;
            }

            @Override
            public ItemStack extractItem(
                    int slot,
                    int amount,
                    boolean simulate
            ) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(
                    int slot,
                    ItemStack stack,
                    boolean simulate
            ) {
                return stack.copy();
            }

            @Override
            public boolean isValid(ServerPlayer player) {
                return true;
            }

            @Override
            public void markChanged() {
            }

            @Override
            public void synchronize(ServerPlayer player) {
            }
        };
    }
}
