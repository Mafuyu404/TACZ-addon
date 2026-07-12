package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterMelee;
import com.mafuyu404.taczaddon.common.ShootWhenReload;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public abstract class LocalPlayerShootMixin {
    @Shadow
    @Final
    private LocalPlayer player;

    @Unique
    private boolean taczaddon$interruptReloadForCurrentShot;

    // Scope the client reload bypass to this shoot invocation.

    @Inject(
            method = "shoot",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void taczaddon$beforeShoot(
            CallbackInfoReturnable<ShootResult> cir
    ) {
        this.taczaddon$interruptReloadForCurrentShot = false;

        if (this.player.getTags().contains("slide")) {
            ModSyncedEntityData.SPRINT_TIME_KEY.setValue(
                    this.player,
                    0.0F
            );
        }

        BetterMelee.onShoot(cir);

        if (cir.isCancelled()) {
            return;
        }

        this.taczaddon$interruptReloadForCurrentShot =
                ShootWhenReload
                        .canInterruptForImmediateShot(this.player);
    }

    @Inject(
            method = "shoot",
            at = @At("RETURN"),
            require = 1
    )
    private void taczaddon$afterShoot(
            CallbackInfoReturnable<ShootResult> cir
    ) {
        this.taczaddon$interruptReloadForCurrentShot = false;
    }

    // Bypass the reload-owned clientStateLock at shoot() entry so the
    // invocation can proceed to the native checks.

    @Redirect(
            method = "shoot",
            at = @At(
                    value = "FIELD",
                    target =
                            "Lcom/tacz/guns/client/gameplay/"
                                    + "LocalPlayerDataHolder;"
                                    + "clientStateLock:Z",
                    ordinal = 0
            ),
            require = 1
    )
    private boolean taczaddon$ignoreReloadStateLock(
            LocalPlayerDataHolder data
    ) {
        if (this.taczaddon$interruptReloadForCurrentShot) {
            return false;
        }

        return data.clientStateLock;
    }

    // Bypass isReloading() inside preCheck(...) for this invocation only.

    @Redirect(
            method = "preCheck",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tacz/guns/api/entity/"
                                    + "ReloadState$StateType;"
                                    + "isReloading()Z"
            ),
            require = 1
    )
    private boolean taczaddon$allowImmediateShotDuringReload(
            ReloadState.StateType stateType
    ) {
        if (this.taczaddon$interruptReloadForCurrentShot) {
            return false;
        }

        return stateType.isReloading();
    }

    // Play only the local cancel animation after every native check passes;
    // the server handles authoritative reload termination and the shot.

    @Inject(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tacz/guns/client/gameplay/"
                                    + "LocalPlayerDataHolder;"
                                    + "lockState(Ljava/util/function/"
                                    + "Predicate;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            require = 1
    )
    private void taczaddon$playLocalCancelAnimationBeforeShotCommit(
            CallbackInfoReturnable<ShootResult> cir
    ) {
        if (!this.taczaddon$interruptReloadForCurrentShot) {
            return;
        }

        ShootWhenReload
                .playLocalReloadInterruptAnimation(
                        this.player
                );

        this.taczaddon$interruptReloadForCurrentShot = false;
    }
}
