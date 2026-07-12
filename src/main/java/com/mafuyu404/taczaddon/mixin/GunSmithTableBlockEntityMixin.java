package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunSmithTableBlockEntity.class, remap = false)
public abstract class GunSmithTableBlockEntityMixin {
    @Inject(method = "createMenu", at = @At("RETURN"))
    private void taczaddon$createCraftingSession(
            int containerId,
            Inventory inventory,
            Player player,
            CallbackInfoReturnable<AbstractContainerMenu> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(cir.getReturnValue() instanceof GunSmithTableMenu menu)) {
            return;
        }

        ResourceLocation blockId = menu.getBlockId();
        if (blockId == null) {
            return;
        }

        BlockPos tablePos =
                ((GunSmithTableBlockEntity) (Object) this).getBlockPos();

        GunSmithCraftingSessionManager.createSession(
                serverPlayer,
                containerId,
                tablePos,
                blockId
        );
    }
}
