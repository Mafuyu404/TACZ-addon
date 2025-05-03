package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.init.VirtualInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class SophisticatedBackpacksCompatInner {
    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);
        InventoryHandler backpack = wrapper.getInventoryHandler();
        int size = wrapper.getBackpack().getTag().getInt("inventorySlots");
        for (int i = 0; i < size; i++) {
            items.add(backpack.getSlotStack(i));
        }
        return items;
    }

    public static void modifyBlockBackpack(ServerPlayer player, BlockPos blockPos, Consumer<IItemHandler> action) {
        BackpackContext.Block backpackContext = new BackpackContext.Block(blockPos);
        modifyBackpack(player, backpackContext, action);
    }

    public static ArrayList<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        ArrayList<ItemStack> items = new ArrayList<>();
//        IBackpackWrapper backpackWrapper = itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(IBackpackWrapper.Noop.INSTANCE);
//                    UUID uuid = backpackWrapper.getContentsUuid().get();
//                    CompoundTag backpack = BackpackStorage.get().getOrCreateBackpackContents(uuid);
//                    ListTag itemList =  backpack.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND);
//        旧方案
        BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
        InventoryHandler handler = backpackWrapper.getInventoryHandler();
        int size = itemStack.getTag().getInt("inventorySlots");
        for (int i = 0; i < size; i++) {
            ItemStack item = handler.getStackInSlot(i);
            items.add(item);
        }
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
            IBackpackWrapper backpackWrapper = itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(IBackpackWrapper.Noop.INSTANCE);
            UUID uuid = backpackWrapper.getContentsUuid().get();
            SBPPacketHandler.INSTANCE.sendToServer(new RequestBackpackInventoryContentsMessage(uuid));
        });
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            if (!backpack.equals(backpackItem)) return false;
            BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, index);
            modifyBackpack(player, backpackContext, action);
            return false;
        });
    }

    public static void modifyBackpack(ServerPlayer player, BackpackContext backpackContext, Consumer<IItemHandler> action) {
        BackpackContainer container = new BackpackContainer(player.containerMenu.containerId + 1, player, backpackContext);
        int size = container.getStorageWrapper().getBackpack().getTag().getInt("inventorySlots");
        VirtualInventory virtualInventory = new VirtualInventory(size, player);
        for (int i = 0; i < size; i++) {
            virtualInventory.setItem(i, container.realInventorySlots.get(i).getItem());
        }
        action.accept(virtualInventory.getHandler());

        InventoryHandler inventoryHandler = container.getStorageWrapper().getInventoryHandler();
        for (int i = 0; i < size; i++) {
            inventoryHandler.extractItem(i, inventoryHandler.getStackInSlot(i).getCount(), false);
            inventoryHandler.insertItem(i, virtualInventory.getItem(i), false);
        }

        container.sendAllDataToRemote();
        UUID uuid = container.getStorageWrapper().getContentsUuid().get();
        CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
        SBPPacketHandler.INSTANCE.sendToClient(player, new BackpackContentsMessage(uuid, backpackContent));
    }

    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            items.add(backpack);
            return false;
        });
        return items;
    }

    public static boolean isBackpackItem(ItemStack itemStack) {
        return (itemStack.getItem() instanceof BackpackItem);
    }

    public static ArrayList<ItemStack> getItemsFromBackpackContext(Player player, BackpackContext backpackContext) {
        ArrayList<ItemStack> items = new ArrayList<>();
        BackpackContainer container = new BackpackContainer(player.containerMenu.containerId + 1, player, backpackContext);
        int size = container.getStorageWrapper().getBackpack().getTag().getInt("inventorySlots");
        for (int i = 0; i < size; i++) {
            items.add(container.realInventorySlots.get(i).getItem());
        }
        return items;
    }
}
