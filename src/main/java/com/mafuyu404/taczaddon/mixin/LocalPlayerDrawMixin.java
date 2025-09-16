package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.client.gameplay.LocalPlayerDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LocalPlayerDraw.class, remap = false)
public class LocalPlayerDrawMixin {
    @ModifyVariable(method = "draw", at = @At("STORE"), ordinal = 1)
    private long modifyPutAwayTime(long time) {
        return Config.FAST_SWAP_GUN.get() ? 0L : time;
    }

    @ModifyVariable(method = "draw", at = @At("STORE"), ordinal = 0)
    private long modifyDrawTime(long time) {
        return Config.FAST_SWAP_GUN.get() ? 0L : time;
    }
}
