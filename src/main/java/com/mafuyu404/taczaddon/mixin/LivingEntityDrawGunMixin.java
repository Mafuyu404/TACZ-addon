package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.entity.shooter.LivingEntityDrawGun;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LivingEntityDrawGun.class, remap = false)
public class LivingEntityDrawGunMixin {
    @ModifyVariable(
            method = "draw(Ljava/util/function/Supplier;)V",
            at = @At("STORE"),
            index = 2,
            remap = false,
            require = 1
    )
    private long modifyDrawTime(long drawTime) {
        return Config.FAST_SWAP_GUN.get() ? 0L : drawTime;
    }
}
