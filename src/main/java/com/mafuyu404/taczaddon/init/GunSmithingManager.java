//package com.mafuyu404.taczaddon.init;
//
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import net.minecraftforge.registries.ForgeRegistries;
//
//import java.util.HashMap;
//import java.util.List;
//
//public class GunSmithingManager {
//    private static final HashMap<String, List<String>> cache =
//            new HashMap<>();
//
//    public static void putCache(String item, List<String> list) {
//        cache.put(item, list);
//    }
//
//    public static String getItemRegistryName(Item item) {
//        if (item == null) {
//            return null;
//        }
//
//        ResourceLocation registryName =
//                ForgeRegistries.ITEMS.getKey(item);
//
//        if (registryName == null) {
//            return null;
//        }
//
//        return registryName.toString();
//    }
//}
