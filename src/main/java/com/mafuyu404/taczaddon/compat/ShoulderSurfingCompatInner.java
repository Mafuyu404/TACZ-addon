package com.mafuyu404.taczaddon.compat;

import com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl;

import static com.github.exopandora.shouldersurfing.api.model.Perspective.SHOULDER_SURFING;

public class ShoulderSurfingCompatInner {
    public static boolean isShoulderSurfing() {
        return ShoulderSurfingImpl.getInstance().isShoulderSurfing();
    }
    public static void enableShoulderSurfing() {
        ShoulderSurfingImpl.getInstance().changePerspective(SHOULDER_SURFING);
    }
}
