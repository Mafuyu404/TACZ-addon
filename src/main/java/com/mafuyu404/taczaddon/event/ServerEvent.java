package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
        NetworkHandler.sendLiberateAttachmentState(serverPlayer);
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = false
    )
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos tablePos = event.getPos();
        BlockEntity blockEntity =
                event.getLevel().getBlockEntity(tablePos);

        if (!(blockEntity instanceof GunSmithTableBlockEntity)) {
            BlockState blockState =
                    event.getLevel().getBlockState(tablePos);
            if (blockState.getBlock()
                    instanceof AbstractGunSmithTableBlock tableBlock) {
                tablePos = tableBlock.getRootPos(tablePos, blockState);
                blockEntity =
                        event.getLevel().getBlockEntity(tablePos);
            }
        }

        if (!(blockEntity instanceof GunSmithTableBlockEntity table)
                || table.isRemoved()) {
            return;
        }

        GunSmithCraftingSessionManager.rememberTableInteraction(
                serverPlayer,
                tablePos,
                table
        );
    }

    @SubscribeEvent
    public static void onContainerOpened(
            PlayerContainerEvent.Open event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (event.getContainer() instanceof GunSmithTableMenu menu) {
            GunSmithCraftingSessionManager.createSessionFromPending(
                    serverPlayer,
                    menu
            );
            return;
        }

        GunSmithCraftingSessionManager.clearPendingInteraction(
                serverPlayer.getUUID()
        );
    }

    @SubscribeEvent
    public static void onContainerClosed(
            PlayerContainerEvent.Close event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getContainer()
                instanceof GunSmithTableMenu menu) {
            GunSmithCraftingSessionManager.removeSession(
                    serverPlayer.getUUID(),
                    menu.containerId
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.clearPlayerState(
                    serverPlayer.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.clearPlayerState(
                    serverPlayer.getUUID()
            );
            NetworkHandler.sendLiberateAttachmentState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GunSmithCraftingSessionManager.clearPlayerState(
                    serverPlayer.getUUID()
            );
            NetworkHandler.sendServerConfig(serverPlayer);
            NetworkHandler.sendLiberateAttachmentState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        GunSmithCraftingSessionManager.removeAll();
    }
}
