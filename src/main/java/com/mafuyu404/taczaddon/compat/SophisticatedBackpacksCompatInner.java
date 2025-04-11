package com.mafuyu404.taczaddon.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.UUID;

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
        BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
//                    UUID uuid = backpackWrapper.getContentsUuid().get();
//                    CompoundTag backpack = BackpackStorage.get().getOrCreateBackpackContents(uuid);
//                    ListTag itemList =  backpack.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND);
        InventoryHandler handler = backpackWrapper.getInventoryHandler();
        int size = itemStack.getTag().getInt("inventorySlots");
        for (int i = 0; i < size; i++) {
            ItemStack item = handler.getSlotStack(i);
            items.add(item);
        }
        return items;
    }

    public static ArrayList<ItemStack> getItemsFromInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        player.getInventory().items.forEach(itemStack -> {
            if (!itemStack.isEmpty()) {
                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                    items.addAll(getItemsFromBackpackItem(itemStack));
//                    String contentsUuid = Arrays.toString(itemStack.getTag().getIntArray("contentsUuid"));
//                    ArrayList<ItemStack> backpack = DataStorage.getBackpack(contentsUuid);
//                    items.addAll(backpack);
                }
            }
        });
        return items;
    }
}
