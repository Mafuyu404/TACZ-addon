package com.mafuyu404.taczaddon.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.Perspective;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ShoulderSurfingCompatInner {
    private ShoulderSurfingCompatInner() {
    }

    public static boolean isShoulderSurfing() {
        return IShoulderSurfing.getInstance().isShoulderSurfing();
    }

    public static void enableShoulderSurfing() {
        IShoulderSurfing.getInstance().changePerspective(Perspective.SHOULDER_SURFING);
    }
}
