package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = GunSmithTableMenu.class, remap = false)
public class GunSmithTableMenuMixin {
    @Redirect(method = "doCraft", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"), remap = false)
    private <T> LazyOptional<T> redirectGetCapability(Player player, Capability<T> capability, Direction facing) {
        ArrayList<ItemStack> containerItems = new ArrayList<>();
        if (player.getPersistentData().contains("BetterGunSmithTable.nearbyContainerPos")) {
            String containerPos = player.getPersistentData().getString("BetterGunSmithTable.nearbyContainerPos");
            String[] Cpos = containerPos.split(";");
            if (containerPos.contains(";")) for (String po : Cpos) {
                String[] parsedPos = po.split(",");
                BlockPos blockPos = new BlockPos(Integer.parseInt(parsedPos[0]), Integer.parseInt(parsedPos[1]), Integer.parseInt(parsedPos[2]));
                ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), blockPos);
                containerItems.addAll(containerContent);
            }
        }
        if (player.getPersistentData().contains("BetterGunSmithTable.nearbyBackpackPos")) {
            String backpackPos = player.getPersistentData().getString("BetterGunSmithTable.nearbyBackpackPos");
            String[] Bpos = backpackPos.split(";");
            if (backpackPos.contains(";")) for (String po : Bpos) {
                String[] parsedPos = po.split(",");
                BlockPos blockPos = new BlockPos(Integer.parseInt(parsedPos[0]), Integer.parseInt(parsedPos[1]), Integer.parseInt(parsedPos[2]));
                ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromBackpackBLock(blockPos, player);
                containerItems.addAll(backpack);
            }
        }
        containerItems.addAll(player.getInventory().items);
        VirtualInventory virtualInventory = new VirtualInventory(containerItems.size(), player);
        for (int i = 0; i < containerItems.size(); i++) {
            virtualInventory.setItem(i, containerItems.get(i));
        }
        return LazyOptional.of(() -> (T) virtualInventory.getHandler());
    }
}
