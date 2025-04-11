package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(value = LivingEntityShoot.class,remap = false)
public class LivingEntityShootMixin {

    @Shadow @Final private ShooterDataHolder data;

    @Shadow @Final private LivingEntity shooter;

    //    @Inject(method = "getShootCoolDown(J)J",at = @At("RETURN"),cancellable = true,remap = false)
//    public void cooldown(long timestamp, CallbackInfoReturnable<Long> cir){
//        if(Config.ENABLED.get()){
//            cir.setReturnValue((long) (((double)cir.getReturnValue()) / Config.TIMES.get()));
//        }
//    }
    @Inject(method = "shoot", at = @At("HEAD"))
    private void slideShoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, CallbackInfoReturnable<ShootResult> cir) {
        if (this.shooter.getTags().contains("slide")) this.data.sprintTimeS = 0.0F;
    }
}
