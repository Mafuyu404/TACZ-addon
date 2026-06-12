package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.compat.JeiCompat;
import com.mafuyu404.taczaddon.compat.ShoulderSurfingCompat;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class SetupEvent {
    private SetupEvent() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ShoulderSurfingCompat::init);
        event.enqueueWork(JeiCompat::init);
        event.enqueueWork(SophisticatedBackpacksCompat::init);
    }
}
