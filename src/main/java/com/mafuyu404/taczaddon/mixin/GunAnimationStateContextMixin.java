package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ReadOnlyCompositeItemHandler;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public class GunAnimationStateContextMixin {
    @Redirect(
            method = "lambda$hasAmmoToConsume$8",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object redirectGetCapability(Entity instance, EntityCapability<IItemHandler, ?> capability, Object context) {
        if (!(instance instanceof LocalPlayer player) || capability != Capabilities.ItemHandler.ENTITY) {
            return instance.getCapability(Capabilities.ItemHandler.ENTITY, null);
        }

        ReadOnlyCompositeItemHandler.Builder builder = ReadOnlyCompositeItemHandler.builder();
        SophisticatedBackpacksCompat.forEachInventoryBackpackHandler(player, handler -> builder.addHandler(handler, "inventory_backpack"));

        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (handler != null) {
            builder.addHandler(handler, "player_inventory");
        }

        return builder.buildReadOnly();
    }
}
