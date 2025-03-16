package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        String storedPos = (String) DataStorage.get("BetterGunSmithTable.nearbyContainerPos");
        String[] pos = storedPos.split(";");
        ArrayList<ItemStack> containerItems = new ArrayList<>();
        for (String po : pos) {
            String[] parsedPos = po.split(",");
            ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), new BlockPos(Integer.parseInt(parsedPos[0]), Integer.parseInt(parsedPos[1]), Integer.parseInt(parsedPos[2])));
            containerItems.addAll(containerContent);
        }
        VirtualInventory virtualInventory = new VirtualInventory(player.getInventory().getContainerSize() + containerItems.size(), player).extend();
        for (int i = 0; i < containerItems.size(); i++) {
            virtualInventory.setItem(virtualInventory.playerInventorySize + i, containerItems.get(i));
        }
//        System.out.print("\n");
//        System.out.print(DataStorage.get("BetterGunSmithTable.nearbyContainerPos"));
//        System.out.print("\n");
        return LazyOptional.of(() -> (T) virtualInventory.getHandler());
    }
}
