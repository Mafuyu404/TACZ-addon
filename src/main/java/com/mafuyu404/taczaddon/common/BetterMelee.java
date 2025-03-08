package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
public class BetterMelee {
    public static void onShoot(CallbackInfoReturnable<ShootResult> cir) {
        if (Config.isItemInBlacklist(Minecraft.getInstance().player.getMainHandItem())) {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(Minecraft.getInstance().player);
            if (!operator.isAim()) {
                operator.melee();
                cir.setReturnValue(ShootResult.ID_NOT_EXIST);
            }
        }
    }
}
