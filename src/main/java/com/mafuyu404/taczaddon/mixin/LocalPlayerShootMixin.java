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