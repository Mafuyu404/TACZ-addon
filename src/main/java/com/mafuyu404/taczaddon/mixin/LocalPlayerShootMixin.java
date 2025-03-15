package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterMelee;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public class LocalPlayerShootMixin {
    @Inject(method = "shoot",at = @At("HEAD"), cancellable = true)
    private void onShoot(CallbackInfoReturnable<ShootResult> cir){
        BetterMelee.onShoot(cir);
//        Player player = Minecraft.getInstance().player;
//        ItemStack mainhandItem = Minecraft.getInstance().player.getMainHandItem();
//        AbstractGunItem gunItem = (AbstractGunItem) mainhandItem.getItem();
//        TimelessAPI.getGunDisplay(Minecraft.getInstance().player.getMainHandItem()).ifPresent(display -> {
//            var animationStateMachine = display.getAnimationStateMachine();
//            if (animationStateMachine != null) {
//                ResourceLocation gunId = gunItem.getGunId(mainhandItem);
//                GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
//                Bolt boltType = gunData.getBolt();
//                boolean noAmmo;
//                if (boltType == Bolt.OPEN_BOLT) {
//                    noAmmo = gunItem.getCurrentAmmoCount(mainhandItem) <= 0;
//                } else {
//                    noAmmo = !gunItem.hasBulletInBarrel(mainhandItem);
//                }
//                // 触发 reload，停止播放声音
//                SoundPlayManager.stopPlayGunSound();
//                SoundPlayManager.playReloadSound(player, display, noAmmo);
//                animationStateMachine.trigger("reload");
////                ObjectAnimation.PlayType pt = ObjectAnimation.PlayType.values()[playType];
//////                runAnimation("idle", track, true, LOOP, 0.3)
////                animationStateMachine.getAnimationController().runAnimation(track, "idle", pt, 0.3);
////                animationStateMachine.getAnimationController().setBlending(track, blending);
//                AnimationStateContext context = animationStateMachine.getContext();
//                context.runAnimation("reload_tactical", 4, false, 1, 0.2F);
//            }
//        });
//        cir.cancel();
//        ShootWhenReload.onShoot(cir);
    }

//    @Inject(method = "shoot", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/entity/IGunOperator;getSynReloadState()Lcom/tacz/guns/api/entity/ReloadState;"))
//    private void onCheckReload(CallbackInfoReturnable<ShootResult> cir) {
//        LocalPlayer tac$player = Minecraft.getInstance().player;
//        LocalPlayerDataHolder tac$data = new LocalPlayerDataHolder(tac$player);
//        LocalPlayerReload tac$reload = new LocalPlayerReload(tac$data, tac$player);
//        tac$reload.cancelReload();
//    }
//
//    @Redirect(method = "shoot", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/entity/ReloadState$StateType;isReloading()Z"))
//    private boolean isReloading(ReloadState.StateType instance) {
//        return false;
//    }
}