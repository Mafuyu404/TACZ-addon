package com.mafuyu404.taczaddon.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;

import static com.github.exopandora.shouldersurfing.api.client.Perspective.SHOULDER_SURFING;

public final class ShoulderSurfingCompatInner {
    private ShoulderSurfingCompatInner() {
    }

    public static boolean isShoulderSurfing() {
        return IShoulderSurfing.getInstance().isShoulderSurfing();
    }
    public static void enableShoulderSurfing() {
        IShoulderSurfing.getInstance().changePerspective(SHOULDER_SURFING);
    }
}
