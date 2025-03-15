package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.SyncEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.HashMap;

public class ClientDataStorage {
    private static final HashMap<Object, Object> storage = new HashMap<>();
    private static final HashMap<String, ArrayList<ItemStack>> backpacks = new HashMap<>();

    public static void set(String key, Object value) {
        storage.put(key, value);
//        SyncEvent event = new SyncEvent(key, value);
//        MinecraftForge.EVENT_BUS.post(event);
    }
    public static Object get(String key) {
        return storage.getOrDefault(key, null);
    }

    public static void setBackpack(String contentsUuid, ArrayList<ItemStack> backpack) {
        for (int i = 0; i < backpack.size(); i++) {
            ItemStack itemStack = backpack.get(i);
            CompoundTag nbt = itemStack.getOrCreateTag();
            nbt.putString("BPindex", contentsUuid + "=>" + i);
            itemStack.setTag(nbt);
//            System.out.print("\n");
//            System.out.print(itemStack.serializeNBT().toString());
//            System.out.print("\n");
        }
        backpacks.put(contentsUuid, backpack);
    }
    public static ArrayList<ItemStack> getBackpack(String contentsUuid) {
        return backpacks.getOrDefault(contentsUuid, new ArrayList<>());
    }
}
