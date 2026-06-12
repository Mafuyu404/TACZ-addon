package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
public class BetterMelee {
    public static void onShoot(CallbackInfoReturnable<ShootResult> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (Config.isItemInBlacklist(player.getMainHandItem())) {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (operator == null) return;

            if (!operator.isAim()) {
                operator.melee();
                cir.setReturnValue(ShootResult.ID_NOT_EXIST);
            }
        }
    }
}
