package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client-side controller for firing immediately after a reload is cancelled.
 *
 * <p>When the player presses fire during a reload, TaCZ 1.1.8 cancels the reload
 * and returns {@code ShootResult.IS_RELOADING} because the synchronized reload
 * state has not transitioned yet. This controller arms a pending shot that fires
 * as soon as the reload state exits reloading.</p>
 *
 * <p>The actual shooting is delegated entirely to TaCZ through
 * {@code IClientPlayerGunOperator.shoot()}, which performs every normal cooldown,
 * ammunition, bolt, and validation check.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ShootAfterReloadCancelController {

    private ShootAfterReloadCancelController() {}

    /**
     * Maximum number of client ticks to wait before expiring a pending shot.
     * Chosen to exceed typical reload-cancellation sync timing (~5-10 ticks).
     */
    private static final long MAX_PENDING_TICKS = 40;

    /**
     * Identifies a specific gun stack so stale references can be detected.
     * Uses hash-and-components to track the stack identity without holding a
     * mutable reference.
     */
    private static int fingerprint(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return ItemStack.hashItemAndComponents(stack);
    }

    @Nullable
    private static PendingReloadShot pending;

    private record PendingReloadShot(
            UUID playerId,
            InteractionHand hand,
            ResourceLocation gunId,
            int selectedSlot,
            int gunStackFingerprint,
            long requestedAtTick,
            long expiresAtTick
    ) {}

    /**
     * Arm a pending shot after TaCZ returns {@code IS_RELOADING}.
     * Called from the mixin at RETURN of {@code LocalPlayerShoot.shoot()}.
     *
     * @param player      the local player
     * @param result      the result returned by TaCZ's shoot()
     * @param gunId       the gun's resource location id
     * @param selectedSlot the player's selected hotbar slot
     */
    public static void armIfReloading(
            LocalPlayer player,
            ShootResult result,
            ResourceLocation gunId,
            int selectedSlot
    ) {
        if (!CommonConfig.enableShootAfterReloadCancel()) {
            return;
        }

        if (result != ShootResult.IS_RELOADING) {
            return;
        }

        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null || !gunId.equals(iGun.getGunId(gunItem))) {
            return;
        }

        // Only one pending shot per player at a time.
        if (pending != null) {
            return;
        }

        long tick = player.level().getGameTime();
        pending = new PendingReloadShot(
                player.getUUID(),
                InteractionHand.MAIN_HAND,
                gunId,
                selectedSlot,
                fingerprint(gunItem),
                tick,
                tick + MAX_PENDING_TICKS
        );
    }

    /**
     * Called on each client tick to check and potentially execute the pending shot.
     */
    public static void onClientTick() {
        if (pending == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            clear();
            return;
        }

        // Validate player identity
        if (!pending.playerId().equals(player.getUUID())) {
            clear();
            return;
        }

        // Validate expiry
        long tick = player.level().getGameTime();
        if (tick > pending.expiresAtTick()) {
            clear();
            return;
        }

        // Validate feature still enabled
        if (!CommonConfig.enableShootAfterReloadCancel()) {
            clear();
            return;
        }

        // Validate screen
        if (mc.screen != null) {
            clear();
            return;
        }

        // Validate player alive
        if (!player.isAlive()) {
            clear();
            return;
        }

        // Validate selected slot unchanged
        if (player.getInventory().selected != pending.selectedSlot()) {
            clear();
            return;
        }

        // Validate main-hand gun unchanged
        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            clear();
            return;
        }

        ResourceLocation currentGunId = iGun.getGunId(gunItem);
        if (!pending.gunId().equals(currentGunId)) {
            clear();
            return;
        }

        if (fingerprint(gunItem) != pending.gunStackFingerprint()) {
            clear();
            return;
        }

        // Check synchronized reload state
        ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
        if (reloadState.getStateType().isReloading()) {
            // Still reloading — wait.
            return;
        }

        // Clear the pending request BEFORE invoking shoot to prevent recursion.
        clear();

        // Delegate to TaCZ's normal shoot path which performs all validation.
        IClientPlayerGunOperator.fromLocalPlayer(player).shoot();
    }

    /**
     * Clear the pending shot. Safe to call from any context.
     */
    public static void clear() {
        pending = null;
    }

    /**
     * Returns whether a pending shot is currently armed.
     */
    public static boolean hasPending() {
        return pending != null;
    }
}
