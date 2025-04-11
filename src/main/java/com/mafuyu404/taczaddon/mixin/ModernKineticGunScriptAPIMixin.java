package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow private LivingEntity shooter;

    @Redirect(method = "lambda$consumeAmmoFromPlayer$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;findAndExtractInventoryAmmos(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int useBackpackAmmo(AbstractGunItem abstractGunItem, IItemHandler cap, ItemStack itemStack, int neededAmount) {
        int cost = abstractGunItem.findAndExtractInventoryAmmos(cap, itemStack, neededAmount);
        ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack((Player) this.shooter);
        backpack.forEach(itemStack1 -> {

        });
        return cost;
    }
}
