package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.mafuyu404.taczaddon.client.GunSmithCompatibilityService;
import com.mafuyu404.taczaddon.client.GunSmithExternalSourceState;
import com.mafuyu404.taczaddon.compat.tacz.api.TaczGunSmithScreenAccess;
import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableSourceViewMixin
        extends AbstractContainerScreen<GunSmithTableMenu>
        implements GunSmithSourceScreenAccess {

    @Shadow
    private Int2IntArrayMap playerIngredientCount;

    @Unique
    private final GunSmithExternalSourceState taczaddon$sourceState =
            new GunSmithExternalSourceState();

    protected GunSmithTableSourceViewMixin(
            GunSmithTableMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Override
    public GunSmithSourceScreenAccess.AcceptResult
    taczaddon$acceptSourceSnapshot(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    ) {
        GunSmithSourceScreenAccess.AcceptResult result =
                this.taczaddon$sourceState.acceptSourceSnapshot(
                containerId,
                requestId,
                sourceRevision,
                externalStacks
        );
        if (result
                == GunSmithSourceScreenAccess.AcceptResult.UPDATED) {
            GunSmithCompatibilityService.applyExternalIngredientCounts(
                    (TaczGunSmithScreenAccess) (Object) this,
                    this.taczaddon$sourceState
                            .getExternalDisplayStacks()
            );
        }
        return result;
    }

    @Override
    public void taczaddon$requestSourceRefresh() {
        this.taczaddon$sourceState.requestSourceRefresh(
                this.menu.containerId
        );
    }

    @Override
    public void taczaddon$tickSourceRefresh() {
        this.taczaddon$sourceState.tickSourceRefresh(
                this.menu.containerId
        );
    }

    @Override
    public void taczaddon$onScreenInit() {
        this.taczaddon$sourceState.onScreenInit(
                this.menu.containerId
        );
    }

    @Override
    public List<ItemStack> taczaddon$getExternalDisplayStacks() {
        return this.taczaddon$sourceState.getExternalDisplayStacks();
    }

    @Inject(
            method = "getPlayerIngredientCount("
                    + "Lcom/tacz/guns/crafting/"
                    + "GunSmithTableRecipe;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void taczaddon$combineExternalSourceCounts(
            GunSmithTableRecipe recipe,
            CallbackInfo ci
    ) {
        List<ItemStack> externalStacks =
                this.taczaddon$sourceState.getExternalDisplayStacks();
        if (externalStacks.isEmpty()
                || Minecraft.getInstance().player == null) {
            return;
        }

        this.playerIngredientCount =
                GunSmithCompatibilityService
                        .computeCombinedIngredientCounts(
                                recipe,
                                Minecraft.getInstance()
                                        .player
                                        .getInventory(),
                                externalStacks
                        );
        ci.cancel();
    }
}
