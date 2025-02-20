package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterAimCamera;
import com.tacz.guns.client.input.AimKey;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AimKey.class, remap = false)
public class AimKeyMixin {
    @Shadow public static KeyMapping AIM_KEY;
    @Inject(method = "onAimPress", at = @At("RETURN"), remap = false)
    private static void onAimPressInject(InputEvent.MouseButton.Post event, CallbackInfo ci) {
        BetterAimCamera.handle(event, AIM_KEY);
    }
}