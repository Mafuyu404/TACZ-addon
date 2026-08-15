package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mafuyu404.taczaddon.init.crafting.WorkbenchAnchor;
import com.mafuyu404.taczaddon.init.crafting.WorkbenchAnchorProvider;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Optional;

/**
 * Default adapter for TaCZ's standard workbench contract.
 *
 * Any addon block that creates a GunSmithTableMenu and uses
 * GunSmithTableBlockEntity gets the same session-anchored source resolution
 * with no mod-specific whitelist entry.
 */
public final class TaczWorkbenchAnchorProvider
        implements WorkbenchAnchorProvider {
    @Override
    public Optional<WorkbenchAnchor> resolve(
            ServerPlayer player,
            AbstractContainerMenu menu
    ) {
        if (!(menu instanceof GunSmithTableMenu tableMenu)) {
            return Optional.empty();
        }

        GunSmithCraftingSessionManager.GunSmithCraftingSession session =
                GunSmithCraftingSessionManager.getSession(
                        player.getUUID()
                );
        if (session == null
                || !session.validate(player, tableMenu.containerId)) {
            return Optional.empty();
        }

        return Optional.of(new WorkbenchAnchor(
                session.getDimension(),
                session.getTablePos()
        ));
    }
}
