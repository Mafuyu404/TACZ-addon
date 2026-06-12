package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.init.ItemStackData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SophisticatedBackpacksCompatInner {
    private SophisticatedBackpacksCompatInner() {
    }

    public static List<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        List<ItemStack> items = new ArrayList<>();
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);
        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }
        addHandlerItems(items, wrapper.getInventoryHandler());
        return items;
    }

    public static void modifyBlockBackpack(ServerPlayer player, BlockPos blockPos, Consumer<IItemHandler> action) {
        forEachBlockBackpackHandler(player, blockPos, action);
    }

    public static void forEachBlockBackpackHandler(Player player, BlockPos blockPos, Consumer<IItemHandler> action) {
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);
        if (wrapper != IBackpackWrapper.Noop.INSTANCE) {
            action.accept(wrapper.getInventoryHandler());
        }
    }

    public static List<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BackpackItem)) {
            return items;
        }

        BackpackWrapper.fromExistingData(itemStack).ifPresent(wrapper -> addHandlerItems(items, wrapper.getInventoryHandler()));
        return items;
    }

    public static List<ItemStack> getItemsFromInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        getAllInventoryBackpack(player).forEach(itemStack -> items.addAll(getItemsFromBackpackItem(itemStack)));
        return items;
    }

    public static void syncAllBackpack(Player player) {
        // Sophisticated Backpacks 1.21.1 replaced the old public packet classes with internal payloads.
        // Reads use current wrappers; forced GUI sync is intentionally unsupported for this migration.
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            if (!ItemStack.isSameItemSameComponents(backpack, backpackItem)) return false;
            BackpackWrapper.fromExistingData(backpack).ifPresent(wrapper -> action.accept(wrapper.getInventoryHandler()));
            return false;
        });
    }

    public static void forEachInventoryBackpackHandler(Player player, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            BackpackWrapper.fromExistingData(backpack).ifPresent(wrapper -> action.accept(wrapper.getInventoryHandler()));
            return false;
        });
    }

    public static List<ItemStack> getAllInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            items.add(backpack.copy());
            return false;
        });
        return items;
    }

    public static boolean isBackpackItem(ItemStack itemStack) {
        return itemStack.getItem() instanceof BackpackItem;
    }

    public static boolean isBackpackBlock(Level level, BlockPos blockPos) {
        return level.isLoaded(blockPos) && level.getBlockEntity(blockPos) instanceof BackpackBlockEntity;
    }

    public static List<ItemStack> getItemsFromBackpackContext(Player player, BackpackContext backpackContext) {
        List<ItemStack> items = new ArrayList<>();
        IBackpackWrapper wrapper = backpackContext.getBackpackWrapper(player);
        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }
        addHandlerItems(items, wrapper.getInventoryHandler());
        return items;
    }

    private static void addHandlerItems(List<ItemStack> items, InventoryHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
    }

    @SuppressWarnings("unused")
    private static CompoundTag getCustomData(ItemStack stack) {
        return ItemStackData.getCustomDataCopy(stack);
    }
}
