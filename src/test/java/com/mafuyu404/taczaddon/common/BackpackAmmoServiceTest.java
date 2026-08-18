package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.testutil.MinecraftTestBootstrap;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackAmmoServiceTest {
    private static FakeAmmoItem COMPATIBLE_AMMO;
    private static FakeAmmoItem INCOMPATIBLE_AMMO;
    private static FakeAmmoBoxItem AMMO_BOX;

    @BeforeAll
    static void bootstrap() throws Exception {
        MinecraftTestBootstrap.prepare();
        COMPATIBLE_AMMO = new FakeAmmoItem(true);
        INCOMPATIBLE_AMMO = new FakeAmmoItem(false);
        AMMO_BOX = new FakeAmmoBoxItem();
        registerIfMissing(
                new ResourceLocation("taczaddon_test", "fake_ammo"),
                COMPATIBLE_AMMO
        );
        registerIfMissing(
                new ResourceLocation(
                        "taczaddon_test",
                        "fake_incompatible_ammo"
                ),
                INCOMPATIBLE_AMMO
        );
        registerIfMissing(
                new ResourceLocation("taczaddon_test", "fake_ammo_box"),
                AMMO_BOX
        );
    }

    @Test
    void ordinaryAmmoIsExtractedDirectlyWithoutTaczMethod() {
        ItemStack gun = new ItemStack(Items.STONE);
        FakeHandler handler = new FakeHandler(
                new ItemStack(COMPATIBLE_AMMO, 12)
        );

        int consumed = BackpackAmmoService
                .extractCompatibleAmmoDirectly(
                        handler,
                        gun,
                        20
                );

        assertEquals(12, consumed);
        assertEquals(0, handler.stacks.get(0).getCount());
    }

    @Test
    void incompatibleAmmoIsNotMutated() {
        ItemStack gun = new ItemStack(Items.STONE);
        FakeHandler handler = new FakeHandler(
                new ItemStack(INCOMPATIBLE_AMMO, 12)
        );

        int consumed = BackpackAmmoService
                .extractCompatibleAmmoDirectly(
                        handler,
                        gun,
                        20
                );

        assertEquals(0, consumed);
        assertEquals(12, handler.stacks.get(0).getCount());
    }

    @Test
    void ammoBoxIsDrainedAndEmptied() {
        ItemStack gun = new ItemStack(Items.STONE);
        ItemStack box = new ItemStack(AMMO_BOX);
        box.getOrCreateTag().putString(
                "AmmoId",
                DefaultAssets.DEFAULT_AMMO_ID.toString()
        );
        box.getOrCreateTag().putInt("AmmoCount", 12);
        FakeHandler handler = new FakeHandler(box);

        int consumed = BackpackAmmoService
                .extractCompatibleAmmoDirectly(
                        handler,
                        gun,
                        20
                );

        assertEquals(12, consumed);
        assertEquals(0, AMMO_BOX.getAmmoCount(box));
        assertEquals(
                DefaultAssets.EMPTY_AMMO_ID,
                AMMO_BOX.getAmmoId(box)
        );
    }

    @Test
    void partialAmmoBoxConsumptionKeepsRemainingAmmo() {
        ItemStack gun = new ItemStack(Items.STONE);
        ItemStack box = new ItemStack(AMMO_BOX);
        box.getOrCreateTag().putString(
                "AmmoId",
                DefaultAssets.DEFAULT_AMMO_ID.toString()
        );
        box.getOrCreateTag().putInt("AmmoCount", 20);
        FakeHandler handler = new FakeHandler(box);

        int consumed = BackpackAmmoService
                .extractCompatibleAmmoDirectly(
                        handler,
                        gun,
                        8
                );

        assertEquals(8, consumed);
        assertEquals(12, AMMO_BOX.getAmmoCount(box));
        assertEquals(
                DefaultAssets.DEFAULT_AMMO_ID,
                AMMO_BOX.getAmmoId(box)
        );
    }

    @Test
    void clampConsumedHandlesNegativeOversizedAndZeroRequests() {
        assertEquals(0, BackpackAmmoService.clampConsumed(10, -5));
        assertEquals(0, BackpackAmmoService.clampConsumed(10, 0));
        assertEquals(10, BackpackAmmoService.clampConsumed(10, 10));
        assertEquals(10, BackpackAmmoService.clampConsumed(10, 99));
        assertEquals(2, BackpackAmmoService.clampConsumed(3, 2));
        assertEquals(0, BackpackAmmoService.clampConsumed(0, 10));
    }

    @Test
    void rawBackpackServiceNoLongerContainsVanillaOrTaczExtractionPath()
            throws Exception {
        String service = Files.readString(
                Path.of("src/main/java/com/mafuyu404/taczaddon/common/"
                        + "BackpackAmmoService.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(service.contains(
                "consumeCompatibleAmmo"
        ));
        assertFalse(service.contains(
                "findAndExtractInventoryAmmo"
        ));
        assertTrue(service.contains(
                "consumeBackpackAmmoRaw"
        ));
        assertTrue(service.contains(
                "extractCompatibleAmmoDirectly"
        ));

        int start = service.indexOf("consumeBackpackAmmoRaw");
        int end = service.indexOf("public static IItemHandler "
                + "createQueryHandler");
        String rawMethod = service.substring(start, end);
        assertFalse(rawMethod.contains("ForgeCapabilities.ITEM_HANDLER"));
        assertFalse(rawMethod.contains("vanilla"));
    }

    private static final class FakeAmmoItem
            extends Item implements IAmmo {
        private final boolean compatible;

        private FakeAmmoItem(boolean compatible) {
            super(new Item.Properties());
            this.compatible = compatible;
        }

        @Override
        public ResourceLocation getAmmoId(ItemStack stack) {
            return DefaultAssets.EMPTY_AMMO_ID;
        }

        @Override
        public void setAmmoId(
                ItemStack stack,
                ResourceLocation ammoId
        ) {
        }

        @Override
        public boolean isAmmoOfGun(
                ItemStack gunStack,
                ItemStack stack
        ) {
            return compatible;
        }
    }

    private static final class FakeAmmoBoxItem
            extends Item implements IAmmoBox {
        private FakeAmmoBoxItem() {
            super(new Item.Properties());
        }

        @Override
        public ResourceLocation getAmmoId(ItemStack stack) {
            String id = stack.getOrCreateTag()
                    .getString("AmmoId");
            if (id.isEmpty()) {
                return DefaultAssets.EMPTY_AMMO_ID;
            }
            return new ResourceLocation(id);
        }

        @Override
        public int getAmmoCount(ItemStack stack) {
            return stack.getOrCreateTag()
                    .getInt("AmmoCount");
        }

        @Override
        public void setAmmoId(
                ItemStack stack,
                ResourceLocation ammoId
        ) {
            stack.getOrCreateTag().putString(
                    "AmmoId",
                    ammoId.toString()
            );
        }

        @Override
        public void setAmmoCount(
                ItemStack stack,
                int ammoCount
        ) {
            stack.getOrCreateTag().putInt(
                    "AmmoCount",
                    ammoCount
            );
        }

        @Override
        public boolean isAmmoBoxOfGun(
                ItemStack gunStack,
                ItemStack stack
        ) {
            return true;
        }

        @Override
        public ItemStack setAmmoLevel(
                ItemStack stack,
                int ammoLevel
        ) {
            return stack;
        }

        @Override
        public int getAmmoLevel(ItemStack stack) {
            return 0;
        }

        @Override
        public boolean isCreative(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isAllTypeCreative(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack setCreative(
                ItemStack stack,
                boolean creative
        ) {
            return stack;
        }
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
        public boolean isItemValid(
                int slot,
                @NotNull ItemStack stack
        ) {
            return false;
        }
    }

    private static void registerIfMissing(
            ResourceLocation id,
            Item item
    ) {
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            ForgeRegistries.ITEMS.register(id, item);
        }
    }
}
