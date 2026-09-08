package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.ContainerReaderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TACZaddon.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void onPlayerLogin(EntityJoinLevelEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        LiberateAttachment.syncRuleWhenLogin(serverPlayer);
        com.mafuyu404.taczaddon.init.NetworkHandler.sendToClient(serverPlayer,
                com.mafuyu404.taczaddon.network.ConfigSyncPacket.fromServerConfig());
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onTableInteraction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().isLoaded(event.getPos())) return;
        var state = player.level().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof com.tacz.guns.block.AbstractGunSmithTableBlock tableBlock)) return;
        var rootPos = tableBlock.getRootPos(event.getPos(), state);
        if (player.level().isLoaded(rootPos)
                && player.level().getBlockEntity(rootPos) instanceof com.tacz.guns.block.entity.GunSmithTableBlockEntity table) {
            com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.rememberTableInteraction(player, rootPos, table);
        }
    }

    @SubscribeEvent
    public static void onMenuOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof com.tacz.guns.inventory.GunSmithTableMenu menu) {
            com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.createSessionFromPending(player, menu);
        } else {
            com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.clearPendingInteraction(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onMenuClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
        com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.removeSession(event.getEntity().getUUID(), event.getContainer().containerId);
        ContainerReaderState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.clearPlayerState(event.getEntity().getUUID());
        ContainerReaderState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.removeAll();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ContainerReaderState.clear(event.getEntity());
        com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager.clearPlayerState(event.getEntity().getUUID());
    }
}
