package com.mafuyu404.taczaddon.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class SophisticatedBackpacksCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";

    private SophisticatedBackpacksCompat() {}

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void init() {
        // Kept for compatibility with existing setup calls.
        // Do not cache mod-loaded state here.
    }

    public static ArrayList<ItemStack> getItemsFromBackpackBLock(BlockPos blockPos, Player player) {
        if (!isInstalled()) return new ArrayList<>();
        return SophisticatedBackpacksCompatInner.getItemsFromBackpackBLock(blockPos, player);
    }

    public static ArrayList<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        if (!isInstalled()) return new ArrayList<>();
        return SophisticatedBackpacksCompatInner.getItemsFromBackpackItem(itemStack);
    }

    public static ArrayList<ItemStack> getItemsFromInventoryBackpack(Player player) {
        if (!isInstalled()) return new ArrayList<>();
        return SophisticatedBackpacksCompatInner.getItemsFromInventoryBackpack(player);
    }

    public static void syncAllBackpack(Player player) {
        if (!isInstalled()) return;
        SophisticatedBackpacksCompatInner.syncAllBackpack(player);
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        if (!isInstalled()) return;
        SophisticatedBackpacksCompatInner.modifyInventoryBackpack(player, backpackItem, action);
    }

    public static void modifyBlockBackpack(ServerPlayer player, BlockPos blockPos, Consumer<IItemHandler> action) {
        if (!isInstalled()) return;
        SophisticatedBackpacksCompatInner.modifyBlockBackpack(player, blockPos, action);
    }

    public static ArrayList<ItemStack> getAllInventoryBackpack(Player player) {
        if (!isInstalled()) return new ArrayList<>();
        return SophisticatedBackpacksCompatInner.getAllInventoryBackpack(player);
    }
}
