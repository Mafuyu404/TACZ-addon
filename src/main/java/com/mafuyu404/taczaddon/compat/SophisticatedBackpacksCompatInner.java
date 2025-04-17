package com.mafuyu404.taczaddon.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
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
import top.theillusivec4.curios.api.CuriosApi;

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

    public static void modifyBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        int size = backpackItem.getTag().getInt("inventorySlots");
        BackpackWrapper backpackWrapper = new BackpackWrapper(backpackItem);
        IBackpackWrapper iBackpackWrapper = backpackItem.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(IBackpackWrapper.Noop.INSTANCE);
        IItemHandler inventoryHandler = backpackWrapper.getInventoryHandler();
        IItemHandler iInventoryHandler = iBackpackWrapper.getInventoryHandler();
        action.accept(iInventoryHandler);
        for (int i = 0; i < size; i++) {
//            System.out.print(iInventoryHandler.getStackInSlot(i)+"/"+inventoryHandler.getStackInSlot(i)+"\n");
            inventoryHandler.extractItem(i, inventoryHandler.getStackInSlot(i).getCount(), false);
            inventoryHandler.insertItem(i, iInventoryHandler.getStackInSlot(i), false);
        }
//        action.accept(inventoryHandler);
        UUID uuid = iBackpackWrapper.getContentsUuid().get();
        CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
        SBPPacketHandler.INSTANCE.sendToClient(player, new BackpackContentsMessage(uuid, backpackContent));
    }

    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                    items.add(itemStack);
                }
            }
        }
        CuriosApi.getCuriosInventory(player).ifPresent(iCuriosItemHandler -> {
            iCuriosItemHandler.findCurios(SophisticatedBackpacksCompatInner::isBackpackItem).forEach(slotResult -> items.add(slotResult.stack()));
        });
        return items;
    }

    public static boolean isBackpackItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        String [] id = itemStack.getItem().getDescriptionId().split("\\.");
        return (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack"));
    }
}
