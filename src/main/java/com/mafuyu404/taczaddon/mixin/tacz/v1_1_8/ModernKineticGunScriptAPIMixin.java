package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.mafuyu404.taczaddon.compat.BeyondIntegrationCompat;
import com.mafuyu404.taczaddon.common.BackpackAmmoService;
import com.mafuyu404.taczaddon.common.AmmoConsumptionOrchestrator;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow private LivingEntity shooter;

    @Shadow private ItemStack itemStack;

    @Shadow private AbstractGunItem abstractGunItem;

    /*
     * Forge 1.20.1 resolves Mixin 0.8.5, whose @Inject annotation does not
     * define the order element. The composition below is order-independent:
     * if Beyond Integration's own RETURN hook runs first, its consumed amount
     * is already visible in cir; if this hook runs first, the controlled
     * PlayerMainInvWrapper bridge gives Beyond the exact compatibility call it
     * expects before the Sophisticated Backpack fallback.
     */
    @Inject(
            method = "consumeAmmoFromPlayer(I)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void taczaddon$consumeBackpackAmmo(
            int neededAmount,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (neededAmount <= 0
                || this.abstractGunItem == null
                || this.shooter == null
                || this.itemStack == null
                || this.itemStack.isEmpty()) {
            return;
        }

        if (this.abstractGunItem.useInventoryAmmo(this.itemStack)
                && !IGunOperator.fromLivingEntity(this.shooter)
                .needCheckAmmo()) {
            return;
        }

        if (this.abstractGunItem.useDummyAmmo(this.itemStack)) {
            return;
        }

        if (!(this.shooter instanceof ServerPlayer player)
                || !SophisticatedBackpacksCompat.isInstalled()) {
            return;
        }

        AbstractGunItem gun = this.abstractGunItem;
        ItemStack gunStack = this.itemStack;
        boolean beyondActive = BeyondIntegrationCompat.isInstalled();
        int consumedSoFar = AmmoConsumptionOrchestrator.clampConsumed(
                neededAmount,
                cir.getReturnValueI()
        );
        int finalConsumed = AmmoConsumptionOrchestrator.consumeRemaining(
                neededAmount,
                consumedSoFar,
                beyondActive,
                beyondActive
                        ? remaining ->
                                BeyondIntegrationCompat
                                        .consumeThroughTaczInventoryContract(
                                                player,
                                                gun,
                                                gunStack,
                                                remaining
                                        )
                        : remaining -> 0,
                remaining -> BackpackAmmoService.consumeBackpackAmmoRaw(
                        player,
                        gunStack,
                        remaining
                )
        );

        if (cir.getReturnValueI() != finalConsumed) {
            cir.setReturnValue(finalConsumed);
        }
    }
}
