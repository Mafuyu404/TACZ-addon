package com.mafuyu404.taczaddon.mixin.tacz;

import com.mafuyu404.taczaddon.compat.ShoulderSurfing5Compat;
import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypasses TaCZ 1.1.8's obsolete SSR 4.x crosshair path when SSR 5.x is
 * installed. This mixin is gated by a TaCZ binary contract and delegates to
 * an optional-safe SSR 5.x facade.
 */
@Mixin(value = ShoulderSurfingCompat.class, remap = false)
public class ShoulderSurfingCompatMixin {
    @Inject(
            method = "showCrosshair",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private static void taczaddon$showCrosshairWithSsr5(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ShoulderSurfing5Compat.isInstalled()) {
            cir.setReturnValue(
                    ShoulderSurfing5Compat
                            .showCrosshairWhenShoulderSurfing()
            );
        }
    }
}
