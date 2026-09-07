package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.CuriosCompat;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ExtractingCompositeItemHandler;
import com.mafuyu404.taczaddon.init.ReadOnlyCompositeItemHandler;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ammo consumption from addon extra storage (inventory backpacks / Curios)
 * plus the player's normal inventory.
 *
 * <p>Version-bound on the stable source method
 * {@code consumeAmmoFromPlayer(I)I} (previously a redirect inside the javac
 * synthetic lambda {@code lambda$consumeAmmoFromPlayer$4}). Only
 * {@link ServerPlayer} shooters are taken over; every other shooter keeps
 * TaCZ's native path.
 *
 * <p>The taken-over logic mirrors TaCZ's own method exactly (inventory-ammo
 * reload exemption, dummy ammo, then inventory extraction). Sophisticated
 * Backpacks are consumed first through the mutation-aware
 * {@code mutateInventoryBackpacks} facade, which snapshots every handler,
 * persists changed handlers and immediately pushes the authoritative contents
 * to the client. Any remaining requested amount is then consumed from the
 * player inventory and Curios through the existing transactional composite.
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {

    @Shadow
    private LivingEntity shooter;

    @Shadow
    private ItemStack itemStack;

    @Shadow
    private AbstractGunItem abstractGunItem;

    @Shadow
    public abstract boolean useInventoryAmmo();

    @Shadow
    public abstract boolean isReloadingNeedConsumeAmmo();

    @Inject(
            method = "consumeAmmoFromPlayer(I)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void taczaddon$consumeAmmoFromPlayer(
            int neededAmount,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(shooter instanceof ServerPlayer player)) {
            return;
        }

        if (useInventoryAmmo()
                && !isReloadingNeedConsumeAmmo()) {
            cir.setReturnValue(neededAmount);
            return;
        }

        if (abstractGunItem.useDummyAmmo(itemStack)) {
            cir.setReturnValue(
                    abstractGunItem.findAndExtractDummyAmmo(
                            itemStack,
                            neededAmount
                    )
            );
            return;
        }

        /*
         * First consume from Sophisticated Backpacks. The facade snapshots
         * every backpack handler, persists mutations and synchronizes the
         * authoritative contents back to the client immediately, so the HUD
         * does not have to wait for periodic polling.
         */
        int[] remaining = {neededAmount};

        SophisticatedBackpacksCompat.mutateInventoryBackpacks(
                player,
                backpackHandler -> {
                    /*
                     * Extract per backpack through the transactional
                     * composite so TaCZ IAmmoBox modifications are applied to
                     * working copies first and written back through
                     * commitChanges(); the surrounding mutation facade then
                     * persists and synchronizes any handler change.
                     */
                    ReadOnlyCompositeItemHandler.Builder backpackBuilder =
                            ReadOnlyCompositeItemHandler.builder();
                    backpackBuilder.addHandler(
                            backpackHandler,
                            "inventory_backpack"
                    );

                    ExtractingCompositeItemHandler extractingHandler =
                            backpackBuilder.buildExtracting();

                    int consumed =
                            abstractGunItem
                                    .findAndExtractInventoryAmmo(
                                            extractingHandler,
                                            itemStack,
                                            remaining[0]
                                    );

                    extractingHandler.commitChanges();

                    remaining[0] -= consumed;

                    return remaining[0] <= 0;
                }
        );

        if (remaining[0] > 0) {
            ReadOnlyCompositeItemHandler.Builder builder =
                    ReadOnlyCompositeItemHandler.builder();

            IItemHandler playerHandler =
                    player.getCapability(
                            Capabilities.ItemHandler.ENTITY
                    );

            if (playerHandler != null) {
                builder.addHandler(
                        playerHandler,
                        "player_inventory"
                );
            }

            CuriosCompat.forEachCuriosHandler(
                    player,
                    handler -> builder.addHandler(
                            handler,
                            "curios"
                    )
            );

            ExtractingCompositeItemHandler extractingHandler =
                    builder.buildExtracting();

            int consumed =
                    abstractGunItem.findAndExtractInventoryAmmo(
                            extractingHandler,
                            itemStack,
                            remaining[0]
                    );

            extractingHandler.commitChanges();

            remaining[0] -= consumed;
        }

        cir.setReturnValue(
                Math.max(0, neededAmount - remaining[0])
        );
    }
}
