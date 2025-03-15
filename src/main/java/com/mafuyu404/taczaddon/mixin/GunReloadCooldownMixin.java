package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.resource.pojo.data.gun.GunReloadTime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunReloadTime.class,remap = false)
public class GunReloadCooldownMixin {

    @Inject(method = {"getEmptyTime","getTacticalTime"},at = @At("RETURN"),cancellable = true,remap = false)
    public void cooldown(CallbackInfoReturnable<Float> cir){
        cir.setReturnValue((float) (cir.getReturnValue() / 3));
    }
}
