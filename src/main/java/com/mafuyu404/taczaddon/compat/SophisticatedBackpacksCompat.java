package com.mafuyu404.taczaddon.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;

public class SophisticatedBackpacksCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

//    public static Inventory get() {
//        if (INSTALLED) {
//            return SophisticatedBackpacksCompatInner.get();
//        }
//        return Minecraft.getInstance().player.getInventory();
//    }

    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromBackpackBLock(blockPos, player);
        }
        return items;
    }
    public static ArrayList<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromBackpackItem(itemStack);
        }
        return items;
    }
    public static ArrayList<ItemStack> getItemsFromInventoryBackpack(Player player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromInventoryBackpack(player);
        }
        return items;
    }
}
