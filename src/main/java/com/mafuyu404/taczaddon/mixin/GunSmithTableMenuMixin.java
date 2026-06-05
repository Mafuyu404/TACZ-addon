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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.function.Consumer;

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
    private <T> LazyOptional<T> tACZ_addon$redirectGetCapability(
            Player player,
            Capability<T> capability,
            Direction facing
    ) {
        if (capability != ForgeCapabilities.ITEM_HANDLER) {
            return player.getCapability(capability, facing);
        }

        ArrayList<ItemStack> containerItems = new ArrayList<>();

        String containerPos = player.getPersistentData().getString("BetterGunSmithTable.nearbyContainerPos");
        this.tACZ_addon$forEachStoredBlockPos(containerPos, blockPos -> {
            ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), blockPos);
            containerItems.addAll(containerContent);
        });

        String backpackPos = player.getPersistentData().getString("BetterGunSmithTable.nearbyBackpackPos");
        this.tACZ_addon$forEachStoredBlockPos(backpackPos, blockPos -> {
            ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromBackpackBLock(blockPos, player);
            containerItems.addAll(backpack);
        });

        containerItems.addAll(player.getInventory().items);

        VirtualInventory virtualInventory = new VirtualInventory(containerItems.size(), player);
        for (int i = 0; i < containerItems.size(); i++) {
            virtualInventory.setItem(i, containerItems.get(i));
        }

        LazyOptional<IItemHandler> result = LazyOptional.of(virtualInventory::getHandler);
        return result.cast();
    }

    @Unique
    private void tACZ_addon$forEachStoredBlockPos(String raw, Consumer<BlockPos> consumer) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        for (String entry : raw.split(";")) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] parts = entry.split(",");
            if (parts.length != 3) {
                continue;
            }

            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                consumer.accept(new BlockPos(x, y, z));
            } catch (NumberFormatException ignored) {
                // Ignore corrupted stored position data.
            }
        }
    }
}
