package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Server-authoritative reload interruption for the shoot-while-reloading
 * feature.
 *
 * The server detects that a shot arrived while the authoritative reload
 * state was still active, bypasses only the reload rejection, runs every
 * other native check, and only after all checks pass terminates the
 * authoritative reload immediately before the shot commits.
 */
@Mixin(
        value = LivingEntityShoot.class,
        remap = false
)
public abstract class
        LivingEntityShootReloadInterruptMixin {

    @Unique
    private static final String
            TACZADDON$SERVER_SHOOT =
            "shoot("
                    + "Ljava/util/function/Supplier;"
                    + "Ljava/util/function/Supplier;"
                    + "JFZ"
                    + ")Lcom/tacz/guns/api/entity/"
                    + "ShootResult;";

    @Shadow
    @Final
    private LivingEntity shooter;

    @Shadow
    @Final
    private ShooterDataHolder data;

    @Unique
    private boolean
            taczaddon$interruptReloadForCurrentShot;

    // Mark this invocation as an interrupt candidate only when all
    // preconditions are met: the shooter is a server player, the
    // server config allows it, and the authoritative reload state
    // is currently active.

    @Inject(
            method = TACZADDON$SERVER_SHOOT,
            at = @At("HEAD"),
            require = 1
    )
    private void taczaddon$beginReloadInterruptShot(
            Supplier<Float> pitch,
            Supplier<Float> yaw,
            long timestamp,
            float chargeProgress,
            boolean hasChargeContext,
            CallbackInfoReturnable<ShootResult> cir
    ) {
        this.taczaddon$interruptReloadForCurrentShot =
                this.shooter instanceof ServerPlayer
                        && CommonConfig
                                .enableShootWhileReloading()
                        && this.data.reloadStateType
                                .isReloading();
    }

    // Bypass only the isReloading() server check for the current
    // invocation. Every other native validation still runs.

    @Redirect(
            method = TACZADDON$SERVER_SHOOT,
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tacz/guns/api/entity/"
                                    + "ReloadState$StateType;"
                                    + "isReloading()Z"
            ),
            require = 1
    )
    private boolean
            taczaddon$allowImmediateServerShot(
                    ReloadState.StateType stateType
            ) {
        if (this.taczaddon$interruptReloadForCurrentShot) {
            return false;
        }

        return stateType.isReloading();
    }

    // End the authoritative reload only after every native shot
    // validation (cooldown, network, draw, bolt, sprint, charge,
    // ammo, overheat, manual-action, Forge event) has passed,
    // immediately before the ServerMessageGunShoot is sent.
    //
    // Calling IGunOperator.cancelReload() first lets the Lua
    // interrupt_reload script update state-machine caches while
    // the gun still thinks it is reloading.  Only after that hook
    // returns do we force NOT_RELOADING.

    @Inject(
            method = TACZADDON$SERVER_SHOOT,
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tacz/guns/network/"
                                    + "NetworkHandler;"
                                    + "sendToTrackingEntity("
                                    + "Ljava/lang/Object;"
                                    + "Lnet/minecraft/world/entity/"
                                    + "Entity;)V",
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void taczaddon$commitReloadInterrupt(
            Supplier<Float> pitch,
            Supplier<Float> yaw,
            long timestamp,
            float chargeProgress,
            boolean hasChargeContext,
            CallbackInfoReturnable<ShootResult> cir
    ) {
        if (!this.taczaddon$interruptReloadForCurrentShot) {
            return;
        }

        IGunOperator.fromLivingEntity(this.shooter)
                .cancelReload();

        this.data.reloadStateType =
                ReloadState.StateType.NOT_RELOADING;

        this.data.reloadTimestamp = -1L;

        ModSyncedEntityData.RELOAD_STATE_KEY.setValue(
                this.shooter,
                new ReloadState()
        );

        this.taczaddon$interruptReloadForCurrentShot =
                false;
    }

    // Always clear the flag so no state leaks to the next invocation,
    // regardless of whether the shot succeeded or was rejected.

    @Inject(
            method = TACZADDON$SERVER_SHOOT,
            at = @At("RETURN"),
            require = 1
    )
    private void taczaddon$finishReloadInterruptShot(
            Supplier<Float> pitch,
            Supplier<Float> yaw,
            long timestamp,
            float chargeProgress,
            boolean hasChargeContext,
            CallbackInfoReturnable<ShootResult> cir
    ) {
        this.taczaddon$interruptReloadForCurrentShot =
                false;
    }
}
