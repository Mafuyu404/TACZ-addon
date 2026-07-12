package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.JeiCompat;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public class JEIEvent {
    @SubscribeEvent
    public static void JEIRecipes(InputEvent.MouseButton event) {
        if (Minecraft.getInstance().screen == null) return;
        if (Minecraft.getInstance().screen instanceof GunSmithTableScreen) {
            if (event.getAction() != InputConstants.RELEASE) return;
            Object data = DataStorage.get("GunSmithTableJEI");
            if (data == null) return;
            ItemStack itemStack = (ItemStack) data;
            if (itemStack.isEmpty()) return;
            boolean result = JeiCompat.showRecipes(itemStack);
            if (result) DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
        }
    }
}
