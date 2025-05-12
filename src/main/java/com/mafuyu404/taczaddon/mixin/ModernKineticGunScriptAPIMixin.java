package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompatInner;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow private LivingEntity shooter;

    @Shadow private ItemStack itemStack;

    @Redirect(method = "lambda$consumeAmmoFromPlayer$4", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;findAndExtractInventoryAmmo(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int useBackpackAmmo(AbstractGunItem abstractGunItem, IItemHandler cap, ItemStack gunItem, int neededAmount) {
        if (!(shooter instanceof Player player)) return abstractGunItem.findAndExtractInventoryAmmo(cap, gunItem, neededAmount);
        final int[] cnt = {neededAmount};
        SophisticatedBackpacksCompat.getAllInventoryBackpack(player).forEach(itemStack -> {
            if (itemStack.isEmpty()) return;
            String[] id = itemStack.getItem().getDescriptionId().split("\\.");
            if (!(id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack"))) return;
            final int[] used = new int[1];
            SophisticatedBackpacksCompat.modifyInventoryBackpack((ServerPlayer) player, itemStack, iItemHandler -> {
                used[0] = abstractGunItem.findAndExtractInventoryAmmo(iItemHandler, gunItem, cnt[0]);
            });
            cnt[0] -= used[0];
        });
        int inventoryAmmoUsed = abstractGunItem.findAndExtractInventoryAmmo(cap, gunItem, cnt[0]);
        cnt[0] -= inventoryAmmoUsed;
        return neededAmount - cnt[0];
    }
}
