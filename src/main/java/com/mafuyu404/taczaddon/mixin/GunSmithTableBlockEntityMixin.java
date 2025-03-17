package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.ContainerMaster;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.ContainerReaderPacket;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(value = GunSmithTableBlockEntity.class, remap = false)
public abstract class GunSmithTableBlockEntityMixin {
    @Shadow public abstract AABB getRenderBoundingBox();

    @Inject(method = "createMenu", at = @At("HEAD"))
    private void iii(int id, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
//        if (FMLEnvironment.dist.isDedicatedServer()) return;
//        System.out.print("\n");
//        System.out.print(FMLEnvironment.dist);
//        System.out.print("\n");
//        ArrayList<ItemStack> items = new ArrayList<>();
//        StringBuilder pos = new StringBuilder();
//        for (int x = (int) this.getRenderBoundingBox().minX; x <= this.getRenderBoundingBox().maxX; x++) {
//            for (int y = (int) this.getRenderBoundingBox().minY; y <= this.getRenderBoundingBox().maxY; y++) {
//                for (int z = (int) this.getRenderBoundingBox().minZ; z <= this.getRenderBoundingBox().maxZ; z++) {
//                    ArrayList<ItemStack> containerContent = ContainerMaster.readContainerFromPos(player.level(), new BlockPos(x, y, z));
//                    if (!containerContent.isEmpty()) {
//                        pos.append(String.format("%s,%s,%s;", x, y, z));
//                        items.addAll(containerContent);
//                    }
//                }
//            }
//        }
//        System.out.print("\n");
//        System.out.print(items.size());
//        System.out.print("\n");
//        DataStorage.set("BetterGunSmithTable.nearbyContainer", items);
//        NetworkHandler.CHANNEL.sendToServer(new PrimitivePacket("BetterGunSmithTable.nearbyContainerPos", pos));
//        NetworkHandler.sendToClient((ServerPlayer) player, new ContainerReaderPacket(items));
    }
}
