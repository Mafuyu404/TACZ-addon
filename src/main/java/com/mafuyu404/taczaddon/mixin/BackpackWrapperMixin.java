package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.AttachmentFromBackpack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = BackpackWrapper.class)
public abstract class BackpackWrapperMixin {
    @Shadow @Nullable private InventoryHandler handler;

    @Shadow public abstract ItemStack getBackpack();

    @Inject(method = "lambda$getInventoryHandler$6", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private void onChange(CallbackInfo ci) {
        System.out.print("\n");
        System.out.print(FMLEnvironment.dist == Dist.CLIENT);
        AttachmentFromBackpack.syncBackpack(this.getBackpack(), this.handler);
    }
}
