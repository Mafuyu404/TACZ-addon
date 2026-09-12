package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.common.AmmoConsumptionOrchestrator.IncompleteConsumptionException;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Deliberately narrow compatibility bridge for Beyond Integration.
 *
 * Beyond Integration augments TaCZ
 * AbstractGunItem#findAndExtractInventoryAmmo at RETURN and identifies
 * player-backed extraction through PlayerMainInvWrapper.
 *
 * TACZ-addon normally keeps Sophisticated Backpack mutation outside that
 * extensible TaCZ extraction path. When Beyond Integration is present we
 * expose exactly one player-main extraction pass here so its existing TaCZ
 * compatibility hook can consume the remaining network ammo before our
 * backpack fallback.
 */
public final class BeyondIntegrationCompat {
    private static final String MOD_ID = "beyond_integration";
    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED = new AtomicBoolean();

    private BeyondIntegrationCompat() {
    }

    public static boolean isInstalled() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    public static int consumeThroughTaczInventoryContract(
            ServerPlayer player,
            AbstractGunItem gun,
            ItemStack gunStack,
            int requested
    ) {
        if (linkageBroken || !isInstalled()
                || player == null
                || gun == null
                || gunStack == null
                || gunStack.isEmpty()
                || requested <= 0) {
            return 0;
        }

        return runGuarded(requested, () -> {
            PlayerMainInvWrapper playerMain =
                    new PlayerMainInvWrapper(player.getInventory());

            return gun.findAndExtractInventoryAmmo(
                    playerMain,
                    gunStack,
                    requested
            );
        });
    }

    static int runGuarded(int requested, IntSupplier operation) {
        if (linkageBroken || requested <= 0) {
            return 0;
        }
        try {
            return clampConsumed(requested, operation.getAsInt());
        } catch (LinkageError error) {
            linkageBroken = true;
            if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
                LogUtils.getLogger().warn(
                        "[TACZ-addon] Beyond Integration ammo bridge unavailable; disabled for this session. "
                                + "Current supplemental consumption stopped because partial extraction is unknown", error);
            }
            // The external hook may already have changed inventory/network state.
            // Do not claim zero consumption and trigger another source for the same deficit.
            throw new IncompleteConsumptionException(error);
        }
    }

    private static int clampConsumed(
            int requested,
            int consumed
    ) {
        if (requested <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(requested, consumed));
    }
}
