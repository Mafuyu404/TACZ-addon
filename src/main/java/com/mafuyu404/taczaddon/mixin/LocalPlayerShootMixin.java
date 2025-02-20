package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Map;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public class LocalPlayerShootMixin {
    @Inject(method = "shoot",at = @At("RETURN"))
    public void get(CallbackInfoReturnable<Long> cir){
//        System.out.print("good");
    }
}