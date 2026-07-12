package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Single owner for Forge-side player/session lifecycle.
 */
@Mod.EventBusSubscriber(
        modid = TACZaddon.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ServerEvent {
    private ServerEvent() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendServerConfig(serverPlayer);
    }

    @SubscribeEvent
    public static void onContainerClosed(
            PlayerContainerEvent.Close event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getContainer()
                instanceof GunSmithTableMenu) {
            GunSmithCraftingSessionManager.removeSession(
                    serverPlayer.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.removeSession(
                    serverPlayer.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.removeSession(
                    serverPlayer.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.removeSession(
                    serverPlayer.getUUID()
            );
            NetworkHandler.sendServerConfig(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        GunSmithCraftingSessionManager.removeAll();
    }
}
