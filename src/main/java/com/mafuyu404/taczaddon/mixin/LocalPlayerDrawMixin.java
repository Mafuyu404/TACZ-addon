package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.client.gameplay.LocalPlayerDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LocalPlayerDraw.class, remap = false)
public class LocalPlayerDrawMixin {
    @ModifyVariable(method = "draw", at = @At("STORE"), name = "putAwayTime")
    private long modifyPutAwayTime(long putAwayTime) {
        return Config.FAST_SWAP_GUN.get() ? 0L : putAwayTime;
    }

    @ModifyVariable(method = "draw", at = @At("STORE"), name = "drawTime")
    private long modifyDrawTime(long drawTime) {
        return Config.FAST_SWAP_GUN.get() ? 0L : drawTime;
    }
}
