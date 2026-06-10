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
import java.util.function.Consumer;

public class SophisticatedBackpacksCompatInner {
    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        BackpackContext.Block context = new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper = context.getBackpackWrapper(player);
        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }

        ItemStack backpackStack = wrapper.getBackpack();
        CompoundTag backpackTag = backpackStack.getTag();
        if (backpackStack.isEmpty() || backpackTag == null) {
            return items;
        }

        InventoryHandler backpack = wrapper.getInventoryHandler();
        int size = backpackTag.getInt("inventorySlots");
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
        CompoundTag backpackTag = itemStack.getTag();
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BackpackItem) || backpackTag == null) {
            return items;
        }

        BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
        InventoryHandler handler = backpackWrapper.getInventoryHandler();
        int size = backpackTag.getInt("inventorySlots");
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
            itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(backpackWrapper ->
                    backpackWrapper.getContentsUuid().ifPresent(uuid ->
                            SBPPacketHandler.INSTANCE.sendToServer(new RequestBackpackInventoryContentsMessage(uuid)))
            );
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
        int size = container.realInventorySlots.size() - player.getInventory().items.size();

        InventoryHandler inventoryHandler = container.getStorageWrapper().getInventoryHandler();

        action.accept(inventoryHandler);

        for (int i = 0; i < size; i++) {
            container.realInventorySlots.get(i).set(inventoryHandler.getStackInSlot(i));
        }

        container.getStorageWrapper().getContentsUuid().ifPresent(uuid -> {
            CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
            SBPPacketHandler.INSTANCE.sendToClient(player, new BackpackContentsMessage(uuid, backpackContent));
        });
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
        ItemStack backpack = container.getStorageWrapper().getBackpack();
        CompoundTag backpackTag = backpack.getTag();
        if (backpack.isEmpty() || backpackTag == null) {
            return items;
        }

        int size = backpackTag.getInt("inventorySlots");
        for (int i = 0; i < size; i++) {
            items.add(container.realInventorySlots.get(i).getItem());
        }
        return items;
    }
}
