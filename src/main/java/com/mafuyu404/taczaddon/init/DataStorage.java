package com.mafuyu404.taczaddon.init;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public class DataStorage {
    private static final HashMap<Object, Object> storage = new HashMap<>();
    private static final HashMap<String, ArrayList<ItemStack>> backpacks = new HashMap<>();

    public static void set(String key, Object value) {
        storage.put(key, value);
    }
    public static Object get(String key) {
        return storage.getOrDefault(key, null);
    }
}
