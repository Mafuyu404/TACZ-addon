package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterMelee;
import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public class LocalPlayerShootMixin {
    @Inject(method = "shoot",at = @At("HEAD"), cancellable = true)
    public void get(CallbackInfoReturnable<ShootResult> cir){
        BetterMelee.onShoot(cir);
    }
}