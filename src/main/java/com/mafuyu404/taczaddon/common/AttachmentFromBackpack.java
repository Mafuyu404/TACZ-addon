package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.client.ClientDataStorage;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class AttachmentFromBackpack {
    // 将单个背包的数据储存到本地
    public static void syncBackpack(ItemStack bpItem, InventoryHandler handler) {
        handler.getStackSizeMultiplier();
        int size = bpItem.getTag().getInt("inventorySlots");
        String contentsUuid = Arrays.toString(bpItem.getTag().getIntArray("contentsUuid"));
        ArrayList<ItemStack> backpack = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack item = handler.getSlotStack(i);
            backpack.add(item);
        }
        ClientDataStorage.setBackpack(contentsUuid, backpack);
    }
    // 玩家登录时将所有背包的内容储存到本地
    public static void syncBackpackWhenLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            String [] id = itemStack.getItem().getDescriptionId().split("\\.");
            if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                BackpackContext.Item context = new BackpackContext.Item("main", i);
                syncBackpack(itemStack, context.getBackpackWrapper(player).getInventoryHandler());
            }
        }
    }
    // 获取玩家所有背包的内容
    public static ArrayList<ItemStack> readAllBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        player.getInventory().items.forEach(itemStack -> {
            if (!itemStack.isEmpty()) {
                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                    String contentsUuid = Arrays.toString(itemStack.getTag().getIntArray("contentsUuid"));
                    ArrayList<ItemStack> backpack = ClientDataStorage.getBackpack(contentsUuid);
                    items.addAll(backpack);
                }
            }
        });
        return items;
    }
    // 更换配件。虚拟物品栏的setItem方法。
    public static void onAttachmentChange(VirtualInventoryChangeEvent.SetItemEvent event) {
        VirtualInventory virtualInventory = event.getVirtualInventory();
        ItemStack gunItem = virtualInventory.getItem(virtualInventory.selected);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        final int[] slot = {event.getSlot()};
        ItemStack oldAttachment = event.getItem();
        ItemStack newAttachement = event.getOriginItem();
        if (!oldAttachment.isEmpty() && iGun != null && iGun.allowAttachment(gunItem, newAttachement)) {
            final int[] index = {0};
            final String[] contentsUuid = new String[1];
            slot[0] -= virtualInventory.playerInventorySize;
            System.out.print(slot[0]);
            System.out.print("\n\n");
            System.out.print(index[0]);
            System.out.print("\n\n");
            virtualInventory.backpackSize.forEach((uuid, size) -> {
                System.out.print(uuid);
                if (slot[0] > index[0]) {
                    contentsUuid[0] = uuid;
                    slot[0] -= index[0];
                    index[0] += size;
                }
            });
            System.out.print(virtualInventory.backpackSize.toString());
            System.out.print("\n\n");
            System.out.print(virtualInventory.backpackIndex.toString());
            System.out.print("\n\n");
            BackpackContext.Item context = new BackpackContext.Item("main", virtualInventory.backpackIndex.get(contentsUuid[0]));
            InventoryHandler handler = context.getBackpackWrapper(Minecraft.getInstance().player).getInventoryHandler();
            handler.setSlotStack(slot[0], oldAttachment);
            handler.saveInventory();
            System.out.print(event.getSlot() + "-" + newAttachement.getHoverName().getString());
            System.out.print("\n");
        }
    }
    // 卸载配件。虚拟物品栏的add方法，理论上只有一处使用。
    public static void onAttachmentUnload(VirtualInventoryChangeEvent.AddEvent event) {
//        VirtualInventory virtualInventory = event.getVirtualInventory();
//        ItemStack gunItem = virtualInventory.getItem(virtualInventory.selected);
//        IGun iGun = IGun.getIGunOrNull(gunItem);
//        ItemStack oldAttachment = event.getItem();
//        if (!oldAttachment.isEmpty() && iGun != null && iGun.allowAttachment(gunItem, oldAttachment)) {
//            Minecraft.getInstance().player.getInventory().add(oldAttachment);
//        }
    }
    public static VirtualInventory useVirtualInventory(Inventory inventory) {
        HashMap<String, Integer> backpackIndex = new HashMap<>();
        HashMap<String, Integer> backpackSize = new HashMap<>();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                    String contentsUuid = Arrays.toString(itemStack.getTag().getIntArray("contentsUuid"));
                    backpackIndex.put(contentsUuid, i);
                }
            }
        }
        final int[] itemCount = {0};
        backpackIndex.forEach((contentsUuid, index) -> {
            ArrayList<ItemStack> backpack = ClientDataStorage.getBackpack(contentsUuid);
            itemCount[0] += backpack.size();
            backpackSize.put(contentsUuid, itemCount[0]);
            items.addAll(backpack);
        });
        int size = itemCount[0] + inventory.getContainerSize();
        VirtualInventory virtualInventory = new VirtualInventory(size, inventory.player);
        virtualInventory.backpackIndex = backpackIndex;
        virtualInventory.backpackSize = backpackSize;
        virtualInventory.selected = inventory.selected;
        for (int i = 0; i < size; i++) {
            if (i < inventory.getContainerSize()) virtualInventory.setItem(i, inventory.getItem(i));
            else virtualInventory.setItem(i, items.get(i - inventory.getContainerSize()));
        }
        return virtualInventory;
    }
}
