package com.mafuyu404.taczaddon.compat;

import net.minecraftforge.fml.ModList;

public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isShoulderSurfing() {
        if (INSTALLED) {
            return ShoulderSurfingCompatInner.isShoulderSurfing();
        }
        return false;
    }

    public static void enableShoulderSurfing() {
        if (INSTALLED) {
            ShoulderSurfingCompatInner.enableShoulderSurfing();
        }
    }
}
