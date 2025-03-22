package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(value = LocalPlayerReload.class, remap = false)
public class LocalPlayerReloadMixin {
    @Inject(method = "doReload", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/statemachine/LuaAnimationStateMachine;trigger(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void nnn(IGun iGun, GunDisplayInstance display, GunData gunData, ItemStack mainhandItem, CallbackInfo ci, LuaAnimationStateMachine<GunAnimationStateContext> animationStateMachine) {
//        String animationName = animationStateMachine.getContext().getAmmoCount() == 0 ? "reload_empty" : "reload_tactical";
//        animationStateMachine.getContext().runAnimation(animationName, 4, false, 1, 0.2F);
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//        Runnable task = () -> {
//            ObjectAnimation animation = animationStateMachine.getAnimationController().getAnimation(4).getAnimation();
//            if (animation.name.contains("reload")) {
//                float maxEndTimeS = animationStateMachine.getAnimationController().getAnimation(4).getAnimation().getMaxEndTimeS();
//                long processNs = animationStateMachine.getAnimationController().getAnimation(4).getProgressNs();
//                float process = Math.round(processNs / 1e7) * 0.01f;
//                float maxEndTime = Math.round(maxEndTimeS * 100) * 0.01f;
//                float reloadSpeedIncrease = 0;
//                animationStateMachine.getContext().adjustAnimationProgress(4, 0.016F * reloadSpeedIncrease, false);
//                if (process == maxEndTime) scheduler.shutdown();
//            }
//        };
//
//        scheduler.scheduleAtFixedRate(task, 16, 16, TimeUnit.MILLISECONDS);
//        ci.cancel();
    }
}
