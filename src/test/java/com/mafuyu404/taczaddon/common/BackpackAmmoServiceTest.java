package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.testutil.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackAmmoServiceTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        MinecraftTestBootstrap.prepare();
    }

    @Test
    void identicalBackpackStacksAreConsumedAcrossBothHandlers() {
        FakeHandler first = new FakeHandler(
                new ItemStack(Items.DIRT, 5)
        );
        FakeHandler second = new FakeHandler(
                new ItemStack(Items.DIRT, 20)
        );

        int consumed = BackpackAmmoService
                .consumeCompatibleAmmoFromHandlers(
                        10,
                        List.of(first, second),
                        extractAllAvailable(),
                        null
                );

        assertEquals(10, consumed);
        assertEquals(0, first.stacks.get(0).getCount());
        assertEquals(15, second.stacks.get(0).getCount());
    }

    @Test
    void ammoOnlyInSecondIdenticalBackpackIsFoundAndConsumed() {
        FakeHandler first = new FakeHandler(ItemStack.EMPTY);
        FakeHandler second = new FakeHandler(
                new ItemStack(Items.DIRT, 20)
        );
        List<FakeHandler> backpacks = List.of(first, second);

        assertTrue(
                BackpackAmmoService.containsCompatibleAmmoInHandlers(
                        backpacks,
                        stack -> !stack.isEmpty()
                )
        );

        int consumed = BackpackAmmoService
                .consumeCompatibleAmmoFromHandlers(
                        10,
                        backpacks,
                        extractAllAvailable(),
                        null
                );

        assertEquals(10, consumed);
        assertEquals(0, first.stacks.get(0).getCount());
        assertEquals(10, second.stacks.get(0).getCount());
    }

    @Test
    void emptyHandlersDoNotReportAmmo() {
        assertFalse(
                BackpackAmmoService.containsCompatibleAmmoInHandlers(
                        List.of(new FakeHandler(ItemStack.EMPTY)),
                        stack -> !stack.isEmpty()
                )
        );
    }

    @Test
    void usedAmountStillClampsNegativeAndOversizedExtraction() {
        assertEquals(0, BackpackAmmoService.usedAmount(10, -5));
        assertEquals(0, BackpackAmmoService.usedAmount(10, 0));
        assertEquals(10, BackpackAmmoService.usedAmount(10, 10));
        assertEquals(10, BackpackAmmoService.usedAmount(10, 99));
        assertEquals(2, BackpackAmmoService.usedAmount(3, 2));
    }

    private static BackpackAmmoService.AmmoExtractor extractAllAvailable() {
        return (handler, remaining) -> {
            int extracted = 0;
            for (int slot = 0;
                 slot < handler.getSlots()
                         && extracted < remaining;
                 slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    int amount = Math.min(
                            stack.getCount(),
                            remaining - extracted
                    );
                    extracted += handler.extractItem(
                            slot,
                            amount,
                            false
                    ).getCount();
                }
            }
            return extracted;
        };
    }

    private static final class FakeHandler implements IItemHandler {
        private final List<ItemStack> stacks;

        private FakeHandler(ItemStack... stacks) {
            this.stacks = new ArrayList<>(List.of(stacks));
        }

        @Override
        public int getSlots() {
            return this.stacks.size();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return this.stacks.get(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(
                int slot,
                @NotNull ItemStack stack,
                boolean simulate
        ) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate
        ) {
            ItemStack stack = this.stacks.get(slot);
            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int extracted = Math.min(amount, stack.getCount());
            ItemStack result = stack.copyWithCount(extracted);
            if (!simulate) {
                this.stacks.set(
                        slot,
                        stack.copyWithCount(
                                stack.getCount() - extracted
                        )
                );
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
