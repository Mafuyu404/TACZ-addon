package com.mafuyu404.taczaddon.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

public class SophisticatedBackpacksCompatInner {
    public static Inventory get() {
        return Minecraft.getInstance().player.getInventory();
    }
    public static void setItem() {

    }
}
