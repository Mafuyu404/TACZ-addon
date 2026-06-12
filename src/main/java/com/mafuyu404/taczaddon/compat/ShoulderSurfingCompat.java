package com.mafuyu404.taczaddon.compat;

import net.neoforged.fml.ModList;

public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        // Kept for existing setup calls; mod-loaded state is checked lazily.
    }

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isShoulderSurfing() {
        if (!isInstalled()) return false;
        return ShoulderSurfingCompatInner.isShoulderSurfing();
    }

    public static void enableShoulderSurfing() {
        if (!isInstalled()) return;
        ShoulderSurfingCompatInner.enableShoulderSurfing();
    }
}
