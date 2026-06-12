package com.mafuyu404.taczaddon.init;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GunSmithingManager {
    private static final HashMap<String, List<String>> cache = new HashMap<>();

    public static void putCache(String item, List<String> list) {
        cache.put(item, list);
    }

    public static List<String> getResult(ItemStack itemStack) {
        CompoundTag nbt = ItemStackData.getCustomDataCopy(itemStack);
        ListTag itemList;
        List<String> result = new ArrayList<>();

        if (nbt.contains("CombinedItems", 9)) {
            itemList = nbt.getList("CombinedItems", 8);
        } else {
            itemList = new ListTag();
        }

        itemList.forEach(tag -> {
            List<String> list = cache.get(tag.getAsString());
            if (list != null) result.addAll(list);
        });

        return result;
    }

    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        if (registryName == null) {
            return null;
        }

        return registryName.toString();
    }
}
