package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.GunSmithDisplayInventory;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import com.mafuyu404.taczaddon.network.GunSmithSourceRefreshRequestPacket;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the complete client-side external-source view:
 *
 * request snapshot -> accept matching response -> recalculate TaCZ counts.
 *
 * The inventory substitution lives in this same Mixin so there is one source
 * of truth for external display stacks.
 */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableSourceViewMixin
        extends AbstractContainerScreen<GunSmithTableMenu>
        implements GunSmithSourceScreenAccess {

    @Shadow
    public abstract void updateIngredientCount();

    @Unique
    private long taczaddon$nextRefreshRequestId;

    @Unique
    private long taczaddon$pendingRefreshRequestId = -1L;

    @Unique
    private long taczaddon$latestAcceptedRefreshRequestId = -1L;

    @Unique
    private long taczaddon$latestSourceRevision = -1L;

    @Unique
    private boolean taczaddon$refreshInFlight;

    @Unique
    private boolean taczaddon$hasAcceptedSnapshot;

    @Unique
    private List<ItemStack> taczaddon$externalDisplayStacks =
            List.of();

    protected GunSmithTableSourceViewMixin(
            GunSmithTableMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Override
    public boolean taczaddon$acceptSourceSnapshot(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    ) {
        if (this.menu.containerId != containerId) {
            return false;
        }

        if (!this.taczaddon$refreshInFlight
                || requestId
                != this.taczaddon$pendingRefreshRequestId) {
            return false;
        }

        if (requestId
                <= this.taczaddon$latestAcceptedRefreshRequestId) {
            this.taczaddon$clearPendingRefresh();
            return false;
        }

        ArrayList<ItemStack> copies = new ArrayList<>();
        if (externalStacks != null) {
            for (ItemStack stack : externalStacks) {
                if (stack != null && !stack.isEmpty()) {
                    copies.add(stack.copy());
                }
            }
        }

        this.taczaddon$externalDisplayStacks =
                Collections.unmodifiableList(copies);
        this.taczaddon$latestAcceptedRefreshRequestId = requestId;
        this.taczaddon$latestSourceRevision = sourceRevision;
        this.taczaddon$hasAcceptedSnapshot = true;
        this.taczaddon$clearPendingRefresh();

        /*
         * TaCZ recalculates ingredient counts and then calls init(). The
         * init-tail hook does not issue another initial request after a
         * snapshot has been accepted, so this cannot form a request loop.
         */
        this.updateIngredientCount();
        return true;
    }

    @Override
    public void taczaddon$requestSourceRefresh() {
        long requestId = ++this.taczaddon$nextRefreshRequestId;

        /*
         * A newer explicit refresh supersedes an older request. This matters
         * when a craft result arrives while an initial refresh is still in
         * flight: the post-craft snapshot must win.
         */
        this.taczaddon$pendingRefreshRequestId = requestId;
        this.taczaddon$refreshInFlight = true;

        NetworkHandler.CHANNEL.sendToServer(
                new GunSmithSourceRefreshRequestPacket(
                        this.menu.containerId,
                        requestId
                )
        );
    }

    @Override
    public List<ItemStack> taczaddon$getExternalDisplayStacks() {
        return this.taczaddon$externalDisplayStacks;
    }

    @ModifyVariable(
            method = "getPlayerIngredientCount",
            at = @At("STORE"),
            ordinal = 0,
            require = 1
    )
    private Inventory taczaddon$extendInventoryForCounting(
            Inventory original
    ) {
        if (this.taczaddon$externalDisplayStacks.isEmpty()) {
            return original;
        }

        return new GunSmithDisplayInventory(
                original.player,
                this.taczaddon$externalDisplayStacks
        );
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void taczaddon$requestInitialSnapshot(
            CallbackInfo ci
    ) {
        if (!this.taczaddon$hasAcceptedSnapshot
                && !this.taczaddon$refreshInFlight) {
            this.taczaddon$requestSourceRefresh();
        }
    }

    @Unique
    private void taczaddon$clearPendingRefresh() {
        this.taczaddon$pendingRefreshRequestId = -1L;
        this.taczaddon$refreshInFlight = false;
    }
}
