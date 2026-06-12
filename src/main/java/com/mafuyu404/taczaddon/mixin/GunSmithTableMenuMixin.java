package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.ContainerReaderState;
import com.mafuyu404.taczaddon.init.ReadOnlyCompositeItemHandler;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunSmithTableMenu.class, remap = false)
public class GunSmithTableMenuMixin {
    @Redirect(
            method = "doCraft",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            remap = false
    )
    private Object taczaddon$redirectGetCapability(Player player, EntityCapability<IItemHandler, ?> capability, Object context) {
        if (capability != Capabilities.ItemHandler.ENTITY) {
            return player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        }

        ReadOnlyCompositeItemHandler.Builder builder = ReadOnlyCompositeItemHandler.builder();

        ContainerReaderState.getSnapshot(player).ifPresent(snapshot -> {
            for (var blockPos : snapshot.containerPositions()) {
                ContainerMaster.getContainerHandler(player.level(), blockPos)
                        .ifPresent(handler -> builder.addHandler(handler, "nearby_container"));
            }
            for (var blockPos : snapshot.backpackPositions()) {
                SophisticatedBackpacksCompat.forEachBlockBackpackHandler(player, blockPos, handler ->
                        builder.addHandler(handler, "nearby_backpack"));
            }
        });

        IItemHandler playerHandler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (playerHandler != null) {
            builder.addHandler(playerHandler, "player_inventory");
        }

        return builder.buildExtracting();
    }
}
