package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(value = AbstractGunItem.class, remap = false)
public class AbstractGunItemMixin {
    @Unique
    private static Player tACZ_addon$shooter;
    @Inject(method = "canReload", at = @At("HEAD"))
    private void storeShooter(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        if (shooter instanceof Player player) tACZ_addon$shooter = player;
        else tACZ_addon$shooter = null;
    }
    @Inject(method = "lambda$canReload$1", at = @At("HEAD"), cancellable = true)
    private static void checkBackpackAmmo(ItemStack gunItem, IItemHandler cap, CallbackInfoReturnable<Boolean> cir){
        if (tACZ_addon$shooter == null) return;
        ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(tACZ_addon$shooter);
        System.out.print(backpack+"\n");
        backpack.forEach(checkAmmoStack -> {
            Item patt4596$temp = checkAmmoStack.getItem();
            if (patt4596$temp instanceof IAmmo iAmmo) {
                if (iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                    cir.setReturnValue(true);
                }
            }
            patt4596$temp = checkAmmoStack.getItem();
            if (patt4596$temp instanceof IAmmoBox iAmmoBox) {
                if (iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                    cir.setReturnValue(true);
                }
            }
        });
    }
}
