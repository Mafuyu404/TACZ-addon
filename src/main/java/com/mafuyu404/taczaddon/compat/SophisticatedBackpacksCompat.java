package com.mafuyu404.taczaddon.compat;

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

    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromBackpackBLock(blockPos, player);
        }
        return new ArrayList<>();
    }
    public static ArrayList<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromBackpackItem(itemStack);
        }
        return new ArrayList<>();
    }
    public static ArrayList<ItemStack> getItemsFromInventoryBackpack(Player player) {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getItemsFromInventoryBackpack(player);
        }
        return new ArrayList<>();
    }
    public static void syncAllBackpack(Player player) {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.syncAllBackpack(player);
        }
    }
    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.modifyInventoryBackpack(player, backpackItem, action);
        }
    }
    public static void modifyBlockBackpack(ServerPlayer player, BlockPos blockPos, Consumer<IItemHandler> action) {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.modifyBlockBackpack(player, blockPos, action);
        }
    }
    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.getAllInventoryBackpack(player);
        }
        return new ArrayList<>();
    }
}
