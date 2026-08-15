package com.mafuyu404.taczaddon.compat;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PerspectiveApiCompatInner {
    private PerspectiveApiCompatInner() {
    }

    public static String currentPerspectiveId() {
        Perspective current = PerspectiveAPI.getCurrent();
        return current == null
                ? null
                : current.info().id();
    }

    public static boolean isFirstPersonActive() {
        return PerspectiveAPI.isCurrent(
                PerspectiveApiCompat.FIRST_PERSON_ID
        );
    }

    public static PerspectiveApiCompat.PerspectiveApiHandle
    requestFirstPerson() {
        PerspectiveOverrideRegistration registration =
                PerspectiveAPI.getOverrideChain().register(
                        10_000,
                        () -> PerspectiveApiCompat.FIRST_PERSON_ID
                );
        return new Handle(registration);
    }

    private record Handle(
            PerspectiveOverrideRegistration registration
    ) implements PerspectiveApiCompat.PerspectiveApiHandle {
        @Override
        public void restore() {
            this.registration.unregister();
        }
    }
}
