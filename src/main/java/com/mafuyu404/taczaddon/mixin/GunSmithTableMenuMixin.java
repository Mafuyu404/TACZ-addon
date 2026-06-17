package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.ContainerReaderState;
import com.mafuyu404.taczaddon.init.ReadOnlyCompositeItemHandler;
import com.mafuyu404.taczaddon.network.ContainerPositionPacket;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunSmithTableMenu.class, remap = false)
public class GunSmithTableMenuMixin {

    @Redirect(
            method = "doCraft",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/world/entity/player/Player;"
                                    + "getCapability("
                                    + "Lnet/neoforged/neoforge/capabilities/"
                                    + "EntityCapability;"
                                    + "Ljava/lang/Object;"
                                    + ")Ljava/lang/Object;"
            ),
            remap = false
    )
    private Object taczaddon$redirectGetCapability(
            Player player,
            EntityCapability<IItemHandler, ?> ignoredCapability,
            Object ignoredContext
    ) {
        ReadOnlyCompositeItemHandler.Builder builder =
                ReadOnlyCompositeItemHandler.builder();

        ContainerReaderState.getSnapshot(player).ifPresent(snapshot -> {
            for (var blockPos : snapshot.containerPositions()) {
                ContainerMaster.getContainerHandler(
                        player.level(),
                        blockPos
                ).ifPresent(handler ->
                        builder.addHandler(
                                handler,
                                "nearby_container"
                        )
                );
            }

            for (var blockPos : snapshot.backpackPositions()) {
                SophisticatedBackpacksCompat.forEachBlockBackpackHandler(
                        player,
                        blockPos,
                        handler -> builder.addHandler(
                                handler,
                                "nearby_backpack"
                        )
                );
            }
        });

        IItemHandler playerHandler =
                player.getCapability(Capabilities.ItemHandler.ENTITY);

        if (playerHandler != null) {
            builder.addHandler(
                    playerHandler,
                    "player_inventory"
            );
        }

        return builder.buildExtracting();
    }

    /**
     * TaCZ has finished validating and extracting ingredients before the
     * final return from doCraft. Re-read the real server-side handlers and
     * push the new counts to the open screen.
     */
    @Inject(
            method =
                    "doCraft("
                            + "Lnet/minecraft/resources/ResourceLocation;"
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + ")V",
            at = @At("TAIL"),
            remap = false
    )
    private void taczaddon$refreshNearbyStorageAfterCraft(
            ResourceLocation recipeId,
            Player player,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            ContainerPositionPacket.refreshStoredSnapshot(serverPlayer);
        }
    }
}