package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.mafuyu404.taczaddon.client.GunSmithCraftBridgeState;
import com.mafuyu404.taczaddon.init.ClientSyncedConfig;
import com.mafuyu404.taczaddon.init.crafting.CraftingTransaction;
import com.mafuyu404.taczaddon.init.crafting.GunSmithCraftScreenAccess;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.annotation.Nullable;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableCraftBridgeMixin
        implements GunSmithCraftScreenAccess {

    @Shadow
    @Nullable
    private GunSmithTableRecipe selectedRecipe;

    @Unique
    private final GunSmithCraftBridgeState taczaddon$craftState =
            new GunSmithCraftBridgeState();

    @Override
    public boolean taczaddon$acceptCraftResult(
            int containerId,
            long requestId,
            boolean success,
            int craftedExecutions,
            ItemStack outputPerCraft,
            @Nullable CraftingTransaction.CraftFailure failure
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != (Object) this
                || !(minecraft.screen instanceof GunSmithTableScreen screen)
                || screen.getMenu().containerId != containerId) {
            return false;
        }
        return this.taczaddon$craftState.acceptCraftResult(
                containerId,
                requestId
        );
    }

    @ModifyArg(
            method = "addCraftButton()V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/gui/components/"
                                    + "ImageButton;<init>(IIIIIII"
                                    + "Lnet/minecraft/resources/"
                                    + "ResourceLocation;"
                                    + "Lnet/minecraft/client/gui/"
                                    + "components/Button$OnPress;)V",
                    remap = true
            ),
            index = 8,
            remap = false,
            require = 1
    )
    private Button.OnPress taczaddon$wrapCraftButton(
            Button.OnPress originalOnPress
    ) {
        return button -> {
            if (this.selectedRecipe == null
                    || Minecraft.getInstance().player == null
                    || !(Minecraft.getInstance().player
                    .containerMenu
                    instanceof GunSmithTableMenu menu)) {
                return;
            }
            ResourceLocation recipeId =
                    this.selectedRecipe.getId();
            this.taczaddon$craftState.requestCraft(
                    menu.containerId,
                    recipeId,
                    Screen.hasShiftDown(),
                    ClientSyncedConfig.batchCraftMax()
            );
        };
    }
}
