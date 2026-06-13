package com.mafuyu404.taczaddon.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.function.Consumer;

public class SophisticatedBackpacksCompatInner {
    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);
        addBackpackItems(items, wrapper);
        return items;
    }

    public static void modifyBlockBackpack(ServerPlayer player, BlockPos blockPos, Consumer<IItemHandler> action) {
        BackpackContext.Block backpackContext = new BackpackContext.Block(blockPos);
        modifyBackpack(player, backpackContext, action);
    }

    public static ArrayList<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (!isBackpackItem(itemStack)) {
            return items;
        }

        itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
                .ifPresent(wrapper -> addBackpackItems(items, wrapper));
        return items;
    }

    public static ArrayList<ItemStack> getItemsFromInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        getAllInventoryBackpack(player).forEach(itemStack -> {
            items.addAll(getItemsFromBackpackItem(itemStack));
        });
        return items;
    }

    public static void syncAllBackpack(Player player) {
        getAllInventoryBackpack(player).forEach(itemStack -> {
            itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(backpackWrapper ->
                    backpackWrapper.getContentsUuid().ifPresent(uuid ->
                            SBPPacketHandler.INSTANCE.sendToServer(new RequestBackpackInventoryContentsMessage(uuid)))
            );
        });
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            if (!ItemStack.matches(backpack, backpackItem)) return false;
            BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, index);
            modifyBackpack(player, backpackContext, action);
            return true;
        });
    }

    public static void modifyBackpack(ServerPlayer player, BackpackContext backpackContext, Consumer<IItemHandler> action) {
        if (!backpackContext.canInteractWith(player)) {
            return;
        }

        IBackpackWrapper wrapper = backpackContext.getBackpackWrapper(player);
        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return;
        }

        InventoryHandler inventoryHandler = wrapper.getInventoryHandler();
        action.accept(inventoryHandler);

        inventoryHandler.saveInventory();
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        syncBackpackContents(player, wrapper);
    }

    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            items.add(backpack.copy());
            return false;
        });
        return items;
    }

    public static boolean isBackpackItem(ItemStack itemStack) {
        return (itemStack.getItem() instanceof BackpackItem);
    }

    public static ArrayList<ItemStack> getItemsFromBackpackContext(Player player, BackpackContext backpackContext) {
        ArrayList<ItemStack> items = new ArrayList<>();
        IBackpackWrapper wrapper = backpackContext.getBackpackWrapper(player);
        addBackpackItems(items, wrapper);
        return items;
    }

    private static void addBackpackItems(ArrayList<ItemStack> items, IBackpackWrapper wrapper) {
        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return;
        }

        InventoryHandler handler = wrapper.getInventoryHandler();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
    }

    private static void syncBackpackContents(ServerPlayer player, IBackpackWrapper wrapper) {
        wrapper.getContentsUuid().ifPresent(uuid -> {
            CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid).copy();
            SBPPacketHandler.INSTANCE.sendToClient(player, new BackpackContentsMessage(uuid, backpackContent));
        });
    }
}
