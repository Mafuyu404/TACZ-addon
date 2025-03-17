package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = GunSmithTableMenu.class, remap = false)
public class GunSmithTableMenuMixin {
    @Redirect(
            method = "doCraft",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"
            ),
            remap = false
    )
    private <T> LazyOptional<T> redirectGetCapability(Player player, Capability<T> capability, Direction facing) {
        String storedPos = player.getPersistentData().getString("BetterGunSmithTable.nearbyContainerPos");
        String[] pos = storedPos.split(";");
        ArrayList<ItemStack> containerItems = new ArrayList<>();
        if (storedPos.contains(";")) for (String po : pos) {
            String[] parsedPos = po.split(",");
            ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), new BlockPos(Integer.parseInt(parsedPos[0]), Integer.parseInt(parsedPos[1]), Integer.parseInt(parsedPos[2])));
            containerItems.addAll(containerContent);
        }
        VirtualInventory virtualInventory = new VirtualInventory(player.getInventory().getContainerSize() + containerItems.size(), player).extend();
        for (int i = 0; i < containerItems.size(); i++) {
            virtualInventory.setItem(virtualInventory.playerInventorySize + i, containerItems.get(i));
        }
        System.out.print("111\n");
        System.out.print(containerItems);
        System.out.print("\n111");
        return LazyOptional.of(() -> (T) virtualInventory.getHandler());
    }

    @Inject(method = "doCraft", at = @At("HEAD"))
    private void qqqqq(ResourceLocation recipeId, Player player, CallbackInfo ci) {
        System.out.print("\n");
        System.out.print(player.getPersistentData().getString("BetterGunSmithTable.nearbyContainerPos"));
        System.out.print("\n");
    }

    @Inject(method = "lambda$doCraft$1", at = @At("HEAD"))
    private void qqqqqq(GunSmithTableRecipe recipe, Player player, IItemHandler handler, CallbackInfo ci) {
        System.out.print("\n");
        System.out.print(recipe);
        System.out.print("\n");
    }
}
