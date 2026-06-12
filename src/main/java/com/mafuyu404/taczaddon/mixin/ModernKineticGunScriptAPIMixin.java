package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ReadOnlyCompositeItemHandler;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow private LivingEntity shooter;

    @Shadow private ItemStack itemStack;

    @Redirect(method = "lambda$consumeAmmoFromPlayer$4", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;findAndExtractInventoryAmmo(Lnet/neoforged/neoforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int useBackpackAmmo(AbstractGunItem abstractGunItem, IItemHandler cap, ItemStack gunItem, int neededAmount) {
        if (!(shooter instanceof ServerPlayer player)) {
            return abstractGunItem.findAndExtractInventoryAmmo(cap, gunItem, neededAmount);
        }

        ReadOnlyCompositeItemHandler.Builder builder = ReadOnlyCompositeItemHandler.builder();
        SophisticatedBackpacksCompat.forEachInventoryBackpackHandler(player, handler -> builder.addHandler(handler, "inventory_backpack"));

        IItemHandler playerHandler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (playerHandler != null) {
            builder.addHandler(playerHandler, "player_inventory");
        } else {
            builder.addHandler(cap, "provided_inventory");
        }

        return abstractGunItem.findAndExtractInventoryAmmo(builder.buildExtracting(), gunItem, neededAmount);
    }
}
