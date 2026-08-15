package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.mafuyu404.taczaddon.common.BackpackAmmoService;
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

    @Inject(
            method = "consumeAmmoFromPlayer(I)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void taczaddon$consumeBackpackAmmo(
            int neededAmount,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (this.abstractGunItem == null
                || this.shooter == null
                || this.itemStack == null
                || this.itemStack.isEmpty()) {
            return;
        }

        if (this.abstractGunItem.useInventoryAmmo(this.itemStack)
                && !IGunOperator.fromLivingEntity(this.shooter)
                .needCheckAmmo()) {
            cir.setReturnValue(neededAmount);
            return;
        }

        if (this.abstractGunItem.useDummyAmmo(this.itemStack)) {
            return;
        }

        if (!(this.shooter instanceof ServerPlayer player)
                || !SophisticatedBackpacksCompat.isInstalled()) {
            return;
        }

        net.minecraftforge.items.IItemHandler vanilla = player
                .getCapability(
                        net.minecraftforge.common.capabilities
                                .ForgeCapabilities.ITEM_HANDLER,
                        null
                )
                .orElse(null);
        cir.setReturnValue(
                BackpackAmmoService.consumeCompatibleAmmo(
                        player,
                        this.abstractGunItem,
                        this.itemStack,
                        neededAmount,
                        vanilla
                )
        );
    }
}
