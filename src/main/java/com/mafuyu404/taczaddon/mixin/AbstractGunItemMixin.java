package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = AbstractGunItem.class, remap = false)
public class AbstractGunItemMixin {
    @Unique
    private static Player tACZ_addon$shooter;

    @Redirect(method = "canReload", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"))
    private <T> LazyOptional<T> checkBackpackAmmos(LivingEntity instance, Capability<T> capability, Direction facing) {
        if (!(instance instanceof Player player)) return instance.getCapability(capability, facing);
        ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(player);
        backpack.addAll(player.getInventory().items);
        VirtualInventory virtualInventory = new VirtualInventory(backpack.size(), player);
        for (int i = 0; i < backpack.size(); i++) {
            virtualInventory.setItem(i, backpack.get(i));
        }
        return LazyOptional.of(() -> (T) virtualInventory.getHandler());
    }
}
