package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.JeiCompat;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = TACZaddon.MODID)
public class SetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(JeiCompat::init);
    }
}
