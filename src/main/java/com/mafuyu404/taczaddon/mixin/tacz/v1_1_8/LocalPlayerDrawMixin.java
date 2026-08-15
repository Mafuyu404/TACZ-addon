package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.mafuyu404.taczaddon.common.FastSwapService;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gameplay.LocalPlayerDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerDraw.class, remap = false)
public abstract class LocalPlayerDrawMixin {
    @Shadow
    @Final
    private LocalPlayerDataHolder data;

    @Inject(
            method = "draw(Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/gameplay/"
                            + "LocalPlayerDraw;resetData()V",
                    shift = At.Shift.AFTER
            ),
            remap = false,
            require = 1
    )
    private void taczaddon$fastSwapDrawStart(CallbackInfo ci) {
        if (FastSwapService.enabled()) {
            this.data.clientDrawTimestamp =
                    System.currentTimeMillis();
        }
    }
}
