package com.mafuyu404.taczaddon.compat;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

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
        if (!isInstalled()
                || player == null
                || gun == null
                || gunStack == null
                || gunStack.isEmpty()
                || requested <= 0) {
            return 0;
        }

        PlayerMainInvWrapper playerMain =
                new PlayerMainInvWrapper(
                        player.getInventory()
                );

        int consumed = gun.findAndExtractInventoryAmmo(
                playerMain,
                gunStack,
                requested
        );

        return clampConsumed(requested, consumed);
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
