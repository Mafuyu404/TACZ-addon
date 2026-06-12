package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.JeiCompat;
import com.mafuyu404.taczaddon.init.ClientSessionState;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public class JEIEvent {
    @SubscribeEvent
    public static void JEIRecipes(InputEvent.MouseButton.Post event) {
        if (!(Minecraft.getInstance().screen instanceof GunSmithTableScreen)) return;
        if (event.getAction() != InputConstants.RELEASE) return;
        ItemStack itemStack = ClientSessionState.getGunSmithJeiStack();
        if (itemStack.isEmpty()) return;
        boolean result = JeiCompat.showRecipes(itemStack);
        if (result) ClientSessionState.setGunSmithJeiStack(ItemStack.EMPTY);
    }
}
