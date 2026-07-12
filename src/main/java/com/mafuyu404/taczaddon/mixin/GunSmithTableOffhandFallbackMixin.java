package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adds offhand-gun fallback to TaCZ's native held-item recipe filtering
 * without changing any compatibility rules.
 *
 * Precedence:
 *  1. main-hand IGun / IAttachment / IAmmo → used as-is (native behavior)
 *  2. offhand IGun                          → used as reference
 *  3. otherwise                             → native unfiltered behavior
 */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableOffhandFallbackMixin {
    @Redirect(
            method = "shouldFilterByMainHand",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/player/LocalPlayer;"
                                    + "getMainHandItem()"
                                    + "Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = true,
            require = 1
    )
    private ItemStack taczaddon$selectFilterStackForAutoEnable(
            LocalPlayer player
    ) {
        return taczaddon$selectFilterReference(player);
    }

    @Redirect(
            method = "isSuitableForMainHand",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/player/LocalPlayer;"
                                    + "getMainHandItem()"
                                    + "Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = true,
            require = 1
    )
    private ItemStack taczaddon$selectFilterStackForRecipe(
            LocalPlayer player
    ) {
        return taczaddon$selectFilterReference(player);
    }

    @Unique
    private static ItemStack taczaddon$selectFilterReference(
            LocalPlayer player
    ) {
        ItemStack mainHand = player.getMainHandItem();
        Item mainItem = mainHand.getItem();

        if (mainItem instanceof IGun
                || mainItem instanceof IAttachment
                || mainItem instanceof IAmmo) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof IGun) {
            return offhand;
        }

        return mainHand;
    }
}
