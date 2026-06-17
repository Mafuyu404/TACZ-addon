package com.mafuyu404.taczaddon.compat;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.*;
import java.util.function.Consumer;

public final class SophisticatedBackpacksCompatInner {
    private SophisticatedBackpacksCompatInner() {
    }

    public static List<ItemStack> getItemsFromBackpackBlock(
            BlockPos blockPos,
            Player player
    ) {
        List<ItemStack> items = new ArrayList<>();

        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);

        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }

        addHandlerItems(items, wrapper.getInventoryHandler());
        return items;
    }

    private static final int MAX_AMMO_COUNT = 9999;

    public static int countInventoryBackpackAmmo(
            Player player,
            ItemStack gunStack
    ) {
        if (player == null || gunStack.isEmpty()) {
            return 0;
        }

        int[] total = {0};

        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    IItemHandler handler =
                            wrapper.getInventoryHandler();

                    for (int slot = 0;
                         slot < handler.getSlots();
                         slot++) {
                        ItemStack candidate =
                                handler.getStackInSlot(slot);

                        if (candidate.isEmpty()) {
                            continue;
                        }

                        if (candidate.getItem() instanceof IAmmo ammo) {
                            if (ammo.isAmmoOfGun(gunStack, candidate)) {
                                total[0] = addAmmoSafely(
                                        total[0],
                                        candidate.getCount()
                                );
                            }
                        }

                        if (candidate.getItem() instanceof IAmmoBox ammoBox) {
                            if (!ammoBox.isAmmoBoxOfGun(
                                    gunStack,
                                    candidate
                            )) {
                                continue;
                            }

                            if (ammoBox.isAllTypeCreative(candidate)
                                    || ammoBox.isCreative(candidate)) {
                                total[0] = MAX_AMMO_COUNT;
                                return true;
                            }

                            total[0] = addAmmoSafely(
                                    total[0],
                                    ammoBox.getAmmoCount(candidate)
                            );
                        }

                        if (total[0] >= MAX_AMMO_COUNT) {
                            return true;
                        }
                    }

                    return false;
                }
        );

        return total[0];
    }

    private static int addAmmoSafely(int current, int amount) {
        if (amount <= 0) {
            return current;
        }

        long result = (long) current + amount;

        return result >= MAX_AMMO_COUNT
                ? MAX_AMMO_COUNT
                : (int) result;
    }

    /**
     * Keep the old misspelled method temporarily so existing callers do not break.
     */
    @Deprecated(forRemoval = false)
    public static List<ItemStack> getItemsFromBackpackBLock(
            BlockPos blockPos,
            Player player
    ) {
        return getItemsFromBackpackBlock(blockPos, player);
    }

    public static void modifyBlockBackpack(
            ServerPlayer player,
            BlockPos blockPos,
            Consumer<IItemHandler> action
    ) {
        forEachBlockBackpackHandler(player, blockPos, action);
    }

    public static void forEachBlockBackpackHandler(
            Player player,
            BlockPos blockPos,
            Consumer<IItemHandler> action
    ) {
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);

        if (wrapper != IBackpackWrapper.Noop.INSTANCE) {
            action.accept(wrapper.getInventoryHandler());
        }
    }

    public static List<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();

        if (itemStack.isEmpty()
                || !(itemStack.getItem() instanceof BackpackItem)) {
            return items;
        }

        /*
         * This method expects the actual backpack stack, not a defensive copy.
         *
         * fromExistingData uses the ItemStack-keyed StorageWrapperRepository
         * and may return empty for copied stacks.
         */
        BackpackWrapper.fromExistingData(itemStack)
                .ifPresent(wrapper ->
                        addHandlerItems(items, wrapper.getInventoryHandler())
                );

        return items;
    }

    public static List<ItemStack> getItemsFromInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();

        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    /*
                     * Use the real stack supplied by PlayerInventoryProvider.
                     * Do not call backpack.copy() before wrapper lookup.
                     *
                     * fromStack guarantees that a wrapper exists. If its
                     * handler was created before synchronization completed,
                     * onContentsNbtUpdated() will clear it when the response
                     * packet is handled.
                     */
                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    addHandlerItems(
                            items,
                            wrapper.getInventoryHandler()
                    );

                    return false;
                }
        );

        return items;
    }

    /**
     * Requests authoritative inventory NBT for every backpack currently
     * accessible through Sophisticated Backpacks' player inventory providers.
     *
     * Client side only.
     */
    public static void syncAllBackpack(Player player) {
        if (!player.level().isClientSide()) {
            return;
        }

        Set<UUID> requestedUuids = new HashSet<>();

        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    UUID uuid = backpack.get(
                            ModCoreDataComponents.STORAGE_UUID.get()
                    );

                    if (uuid == null || !requestedUuids.add(uuid)) {
                        return false;
                    }

                    /*
                     * Register a wrapper for this exact ItemStack instance.
                     *
                     * Do not initialize its InventoryHandler here because the
                     * client BackpackStorage may not contain the synchronized
                     * NBT yet.
                     */
                    BackpackWrapper.fromStack(backpack);

                    PacketDistributor.sendToServer(
                            new RequestBackpackInventoryContentsPayload(uuid)
                    );

                    return false;
                }
        );
    }

    /**
     * Called after BackpackContentsPayload has written the received NBT into
     * the client-side BackpackStorage.
     *
     * Returns true when a carried backpack with the UUID was found.
     */
    public static boolean refreshInventoryBackpackWrapper(
            Player player,
            UUID updatedUuid
    ) {
        if (updatedUuid == null) {
            return false;
        }

        boolean[] refreshed = {false};

        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    UUID backpackUuid = backpack.get(
                            ModCoreDataComponents.STORAGE_UUID.get()
                    );

                    if (!updatedUuid.equals(backpackUuid)) {
                        return false;
                    }

                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    if (wrapper instanceof BackpackWrapper backpackWrapper) {
                        /*
                         * Clears the cached InventoryHandler and UpgradeHandler.
                         * Their next access reloads data from BackpackStorage.
                         */
                        backpackWrapper.onContentsNbtUpdated();
                        refreshed[0] = true;
                    }

                    return true;
                }
        );

        return refreshed[0];
    }

    public static void modifyInventoryBackpack(
            ServerPlayer player,
            ItemStack backpackItem,
            Consumer<IItemHandler> action
    ) {
        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    if (!ItemStack.isSameItemSameComponents(
                            backpack,
                            backpackItem
                    )) {
                        return false;
                    }

                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    action.accept(wrapper.getInventoryHandler());
                    return false;
                }
        );
    }

    public static void forEachInventoryBackpackHandler(
            Player player,
            Consumer<IItemHandler> action
    ) {
        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    action.accept(wrapper.getInventoryHandler());
                    return false;
                }
        );
    }

    /**
     * Returns defensive copies for callers that only need backpack item
     * snapshots. Do not use these copies for StorageWrapperRepository lookups.
     */
    public static List<ItemStack> getAllInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();

        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    items.add(backpack.copy());
                    return false;
                }
        );

        return items;
    }

    public static boolean isBackpackItem(ItemStack itemStack) {
        return itemStack.getItem() instanceof BackpackItem;
    }

    public static boolean isBackpackBlock(
            Level level,
            BlockPos blockPos
    ) {
        return level.isLoaded(blockPos)
                && level.getBlockEntity(blockPos)
                instanceof BackpackBlockEntity;
    }

    public static List<ItemStack> getItemsFromBackpackContext(
            Player player,
            BackpackContext backpackContext
    ) {
        List<ItemStack> items = new ArrayList<>();

        IBackpackWrapper wrapper =
                backpackContext.getBackpackWrapper(player);

        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }

        addHandlerItems(items, wrapper.getInventoryHandler());
        return items;
    }

    private static void addHandlerItems(
            List<ItemStack> items,
            IItemHandler handler
    ) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);

            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
    }
}
