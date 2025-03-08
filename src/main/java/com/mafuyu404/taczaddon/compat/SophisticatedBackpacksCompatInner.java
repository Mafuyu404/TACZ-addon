package com.mafuyu404.taczaddon.compat;

public class SophisticatedBackpacksCompatInner {
    public static void get() {

    }
    public static void setItem() {

    }
    // 获取玩家所有背包的内容
//    public static ArrayList<ItemStack> readAllBackpack(Player player) {
//        ArrayList<ItemStack> items = new ArrayList<>();
//        player.getInventory().items.forEach(itemStack -> {
//            if (!itemStack.isEmpty()) {
//                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
//                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
//                    BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
//                    UUID uuid = backpackWrapper.getContentsUuid().get();
////                    CompoundTag backpack = BackpackStorage.get().getOrCreateBackpackContents(uuid);
////                    ListTag itemList =  backpack.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND);
//                    InventoryHandler handler = backpackWrapper.getInventoryHandler();
//                    int size = itemStack.getTag().getInt("inventorySlots");
//                    for (int i = 0; i < size; i++) {
//                        ItemStack item = handler.getSlotStack(i);
//                        CompoundTag nbt = item.getOrCreateTag();
//                        nbt.putString("BPindex", uuid + "=>" + i);
//                        item.setTag(nbt);
//                        items.add(item);
//                    }
////                    String contentsUuid = Arrays.toString(itemStack.getTag().getIntArray("contentsUuid"));
////                    ArrayList<ItemStack> backpack = ClientDataStorage.getBackpack(contentsUuid);
////                    items.addAll(backpack);
//                }
//            }
//        });
//        return items;
//    }
}
