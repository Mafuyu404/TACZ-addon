package com.mafuyu404.taczaddon.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.ModList;

public class SophisticatedBackpacksCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static Inventory get() {
        if (INSTALLED) {
            return SophisticatedBackpacksCompatInner.get();
        }
        return Minecraft.getInstance().player.getInventory();
    }

    public static void setItem() {
        if (INSTALLED) {
            SophisticatedBackpacksCompatInner.setItem();
        }
    }
}
