package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class AttachmentFromBackpack {
    // 将单个背包的数据更新到本地
//    public static void syncBackpack(ItemStack bpItem, InventoryHandler handler) {
//        handler.getStackSizeMultiplier();
//        int size = bpItem.getTag().getInt("inventorySlots");
//        String contentsUuid = Arrays.toString(bpItem.getTag().getIntArray("contentsUuid"));
//        ArrayList<ItemStack> backpack = new ArrayList<>();
//        for (int i = 0; i < size; i++) {
//            ItemStack item = handler.getSlotStack(i);
//            backpack.add(item);
//        }
//        ClientDataStorage.setBackpack(contentsUuid, backpack);
//    }
    // 玩家登录时将所有背包的内容储存到本地
    public static void syncBackpackWhenLogin(PlayerEvent.PlayerLoggedInEvent event) {
//        ServerPlayer player = (ServerPlayer) event.getEntity();
//        Inventory inventory = player.getInventory();
//        for (int i = 0; i < inventory.items.size(); i++) {
//            ItemStack itemStack = inventory.getItem(i);
//            String [] id = itemStack.getItem().getDescriptionId().split("\\.");
//            if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
//                BackpackContext.Item context = new BackpackContext.Item("main", i);
//                syncBackpack(itemStack, context.getBackpackWrapper(player).getInventoryHandler());
//            }
//        }
    }
    // 更换配件。虚拟物品栏的setItem方法。
    public static void onAttachmentChange(VirtualInventoryChangeEvent.SetItemEvent event) {
////        System.out.print("\n");
////        System.out.print(FMLEnvironment.dist == Dist.CLIENT);
////        System.out.print("\n");
//        Player player = Minecraft.getInstance().player;
//        VirtualInventory virtualInventory = event.getVirtualInventory();
//        ItemStack gunItem = virtualInventory.getItem(virtualInventory.selected);
//        IGun iGun = IGun.getIGunOrNull(gunItem);
//        ItemStack oldAttachment = event.getItem();
//        ItemStack newAttachement = event.getOriginItem();
//        if (iGun != null && iGun.allowAttachment(gunItem, newAttachement)) {
//            CompoundTag nbt = newAttachement.getOrCreateTag();
//            String [] BPindex = nbt.getString("BPindex").split("=>");
////            System.out.print("\n");
////            System.out.print(nbt.getString("BPindex"));
////            System.out.print("\n");
//            String BPuuid = BPindex[0];
//            int ItemSlot = Integer.parseInt(BPindex[1]);
//            int BPslot = 0;
//            for (int i = 0; i < virtualInventory.playerInventorySize; i++) {
//                ItemStack itemStack = player.getInventory().getItem(i);
//                if (!itemStack.isEmpty()) {
//                    String [] id = itemStack.getItem().getDescriptionId().split("\\.");
//                    if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
//                        String contentsUuid = new BackpackWrapper(itemStack).getContentsUuid().get().toString();
//                        if (contentsUuid.equals(BPuuid)) {
//                            BPslot = i;
//                            break;
//                        }
//                    }
//                }
//            }
//            ItemStack BPitem = player.getInventory().getItem(BPslot);
////            BackpackWrapper backpackWrapper = new BackpackWrapper(BPitem);
////            InventoryHandler handler = backpackWrapper.getInventoryHandler();
////            UUID uuid = backpackWrapper.getContentsUuid().get();
////            CompoundTag inventory = backpack.getCompound("inventory");
////            ListTag items = inventory.getList("Items", Tag.TAG_COMPOUND);
////            int index = 0;
////            for (int i = 0; i < items.size(); i++) {
////                CompoundTag item = (CompoundTag) items.get(i);
////                if (item.getInt("Slot") == ItemSlot) {
////                    index = i;
////                }
////            }
////            if (oldAttachment.isEmpty()) items.remove(index);
////            else items.set(index, oldAttachment.serializeNBT());
////            System.out.print("\n");
////            System.out.print(items);
////            System.out.print("\n");
////            if (oldAttachment.isEmpty()) handler.setSlotStack(ItemSlot, ItemStack.EMPTY);
////            handler.setSlotStack(ItemSlot, oldAttachment);
////            handler.saveInventory();
////            CompoundTag backpack = BackpackStorage.get().getOrCreateBackpackContents(uuid);
//            CHANNEL.sendToServer(new CommonMessagePacket(BPitem, ItemSlot, oldAttachment));
////            SBPPacketHandler.INSTANCE.sendToServer(new RequestBackpackInventoryContentsMessage(uuid));
//            System.out.print(event.getSlot() + "-" + newAttachement.getHoverName().getString());
//            System.out.print("\n");
//        }
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
    // 枪械读取配件时顺便也读读背包里的。
    public static VirtualInventory useVirtualInventory(Inventory inventory) {
//        ArrayList<ItemStack> items = readAllBackpack(inventory.player);
//        int size = items.size() + inventory.getContainerSize();
//        VirtualInventory virtualInventory = new VirtualInventory(size, inventory.player);
//        virtualInventory.selected = inventory.selected;
//        for (int i = 0; i < size; i++) {
//            if (i < inventory.getContainerSize()) virtualInventory.setItem(i, inventory.getItem(i));
//            else virtualInventory.setItem(i, items.get(i - inventory.getContainerSize()));
//        }
//        return virtualInventory;
        return null;
    }
}
