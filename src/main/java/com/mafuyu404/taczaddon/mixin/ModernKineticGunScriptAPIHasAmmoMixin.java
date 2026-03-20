package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIHasAmmoMixin {
    @Redirect(
            method = "hasAmmoToConsume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"
            )
    )
    private <T> LazyOptional<T> redirectGetCapability(LivingEntity instance, net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.core.Direction facing) {
        if (!(instance instanceof Player player) || capability != ForgeCapabilities.ITEM_HANDLER) {
            return instance.getCapability(capability, facing);
        }

        ArrayList<ItemStack> allItems = new ArrayList<>();
        ArrayList<ItemStack> backpacks = SophisticatedBackpacksCompat.getAllInventoryBackpack(player);
        for (ItemStack backpack : backpacks) {
            if (!backpack.isEmpty()) {
                allItems.addAll(SophisticatedBackpacksCompat.getItemsFromBackpackItem(backpack));
            }
        }

        instance.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                allItems.add(handler.getStackInSlot(i));
            }
        });

        VirtualInventory virtualInventory = new VirtualInventory(allItems.size(), player);
        for (int i = 0; i < allItems.size(); i++) {
            virtualInventory.setItem(i, allItems.get(i));
        }

        return LazyOptional.of(() -> (T) virtualInventory.getHandler());
    }
}