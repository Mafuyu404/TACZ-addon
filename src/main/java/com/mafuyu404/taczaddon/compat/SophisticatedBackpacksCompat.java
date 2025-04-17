package com.mafuyu404.taczaddon.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.function.Consumer;

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
    public static void syncAllBackpack(Player player) {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.syncAllBackpack(player);
        }
    }
    public static void modifyBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.modifyBackpack(player, backpackItem, action);
        }
    }
    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getAllInventoryBackpack(player);
        }
        return new ArrayList<>();
    }
}
