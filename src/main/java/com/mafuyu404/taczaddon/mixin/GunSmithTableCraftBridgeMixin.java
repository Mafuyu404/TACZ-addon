package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.ClientSyncedConfig;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.crafting.CraftingTransaction;
import com.mafuyu404.taczaddon.init.crafting.GunSmithCraftScreenAccess;
import com.mafuyu404.taczaddon.network.GunSmithCraftRequestPacket;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableCraftBridgeMixin
        implements GunSmithCraftScreenAccess {

    @Unique
    private static final long taczaddon$PENDING_TIMEOUT_MS = 10_000L;

    @Shadow
    @Nullable
    private GunSmithTableRecipe selectedRecipe;

    @Unique
    private long taczaddon$nextCraftRequestId;

    @Unique
    private long taczaddon$pendingCraftRequestId = -1L;

    @Unique
    private long taczaddon$pendingSinceMs;

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

        if (minecraft.screen != (Object) this) {
            return false;
        }

        if (!(minecraft.screen instanceof GunSmithTableScreen screen)
                || screen.getMenu().containerId != containerId) {
            return false;
        }

        if (requestId != this.taczaddon$pendingCraftRequestId) {
            return false;
        }

        this.taczaddon$pendingCraftRequestId = -1L;
        this.taczaddon$pendingSinceMs = 0L;
        return true;
    }

    @Inject(
            method = "lambda$addCraftButton$5",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void taczaddon$interceptCraft(
            Button button,
            CallbackInfo ci
    ) {
        ci.cancel();

        if (this.selectedRecipe == null) {
            return;
        }

        ResourceLocation recipeId = this.selectedRecipe.getId();
        if (recipeId == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != (Object) this
                || minecraft.player == null
                || !(minecraft.player.containerMenu
                instanceof GunSmithTableMenu menu)) {
            return;
        }

        long now = Util.getMillis();
        if (this.taczaddon$pendingCraftRequestId >= 0L) {
            if (now - this.taczaddon$pendingSinceMs
                    < taczaddon$PENDING_TIMEOUT_MS) {
                return;
            }

            this.taczaddon$pendingCraftRequestId = -1L;
        }

        int requestedCount = Screen.hasShiftDown()
                ? Math.max(1, ClientSyncedConfig.batchCraftMax())
                : 1;

        long requestId = ++this.taczaddon$nextCraftRequestId;
        this.taczaddon$pendingCraftRequestId = requestId;
        this.taczaddon$pendingSinceMs = now;

        NetworkHandler.CHANNEL.sendToServer(
                new GunSmithCraftRequestPacket(
                        menu.containerId,
                        requestId,
                        recipeId,
                        requestedCount
                )
        );
    }
}
