package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.ClientSyncedConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

/**
 * Binary compatibility facade for integrations compiled against TACZ-addon
 * 1.1.6. New code must use {@link LiberateAttachmentService} directly.
 */
@Deprecated(forRemoval = false)
public final class LiberateAttachment {
    private LiberateAttachment() {
    }

    /**
     * Delegates to the current short-lived virtual catalog implementation.
     */
    @Deprecated(forRemoval = false)
    public static Inventory useVirtualInventory(Inventory inventory) {
        return LiberateAttachmentService.createInventory(
                inventory,
                isLiberated(inventory.player)
        );
    }

    /**
     * Returns the authoritative gamerule on the logical server and the
     * fail-closed synchronized mirror on the logical client.
     */
    @Deprecated(forRemoval = false)
    public static boolean isLiberated(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return LiberateAttachmentService.isEnabled(serverPlayer);
        }
        if (!player.level().isClientSide) {
            return false;
        }
        return DistExecutor.unsafeRunForDist(
                () -> ClientState::isLiberated,
                () -> () -> false
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientState {
        private ClientState() {
        }

        private static boolean isLiberated() {
            return ClientSyncedConfig.liberateAttachment();
        }
    }
}
