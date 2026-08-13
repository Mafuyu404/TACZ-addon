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

    /*
     * Refresh every 30 ticks (1.5s) while the workbench screen is active.
     * A radius-16 scan is up to 3267 candidate positions, so this deliberately
     * avoids per-frame or per-tick polling and never overlaps an in-flight
     * request.
     */
    @Unique
    private static final int taczaddon$REFRESH_INTERVAL_TICKS = 30;

    /*
     * A refresh normally returns almost immediately over Minecraft's reliable
     * connection. This timeout is not a session lifetime: it only prevents a
     * lost/aborted response from leaving refreshInFlight stuck forever.
     */
    @Unique
    private static final int taczaddon$REFRESH_TIMEOUT_TICKS = 100;

    @Shadow
    public abstract void updateIngredientCount();

    @Unique
    private long taczaddon$nextRefreshRequestId;

    @Unique
    private int taczaddon$trackedContainerId = Integer.MIN_VALUE;

    @Unique
    private int taczaddon$ticksUntilRefresh =
            taczaddon$REFRESH_INTERVAL_TICKS;

    @Unique
    private long taczaddon$pendingRefreshRequestId = -1L;

    @Unique
    private long taczaddon$latestAcceptedRefreshRequestId = -1L;

    @Unique
    private long taczaddon$latestSourceRevision = -1L;

    @Unique
    private boolean taczaddon$refreshInFlight;

    @Unique
    private int taczaddon$pendingRefreshAgeTicks;

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
        this.taczaddon$ticksUntilRefresh =
                taczaddon$REFRESH_INTERVAL_TICKS;

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
        this.taczaddon$pendingRefreshAgeTicks = 0;

        NetworkHandler.CHANNEL.sendToServer(
                new GunSmithSourceRefreshRequestPacket(
                        this.menu.containerId,
                        requestId
                )
        );
    }

    @Override
    public void taczaddon$tickSourceRefresh() {
        if (this.taczaddon$trackedContainerId
                != this.menu.containerId) {
            return;
        }

        if (this.taczaddon$refreshInFlight) {
            this.taczaddon$pendingRefreshAgeTicks++;
            if (this.taczaddon$pendingRefreshAgeTicks
                    < taczaddon$REFRESH_TIMEOUT_TICKS) {
                return;
            }

            /*
             * Only the display refresh request timed out. Keep the gunsmith
             * session itself untouched and supersede the abandoned request
             * with a new request id. Any late old response is rejected by the
             * pending-request check in acceptSourceSnapshot().
             */
            this.taczaddon$clearPendingRefresh();
            this.taczaddon$requestSourceRefresh();
            return;
        }

        if (--this.taczaddon$ticksUntilRefresh > 0) {
            return;
        }

        this.taczaddon$ticksUntilRefresh =
                taczaddon$REFRESH_INTERVAL_TICKS;
        this.taczaddon$requestSourceRefresh();
    }

    @Override
    public List<ItemStack> taczaddon$getExternalDisplayStacks() {
        return this.taczaddon$externalDisplayStacks;
    }

    @ModifyVariable(
            method =
                    "getPlayerIngredientCount("
                            + "Lcom/tacz/guns/crafting/"
                            + "GunSmithTableRecipe;)V",
            at = @At("STORE"),
            ordinal = 0,
            remap = false,
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

    @Inject(
            method = "init()V",
            at = @At("TAIL"),
            remap = true,
            require = 1
    )
    private void taczaddon$requestInitialSnapshot(
            CallbackInfo ci
    ) {
        if (this.taczaddon$trackedContainerId
                != this.menu.containerId) {
            this.taczaddon$resetForContainer(
                    this.menu.containerId
            );
        }

        if (!this.taczaddon$hasAcceptedSnapshot
                && !this.taczaddon$refreshInFlight) {
            this.taczaddon$requestSourceRefresh();
        }
    }

    @Unique
    private void taczaddon$resetForContainer(int containerId) {
        this.taczaddon$clearPendingRefresh();
        this.taczaddon$trackedContainerId = containerId;
        this.taczaddon$ticksUntilRefresh =
                taczaddon$REFRESH_INTERVAL_TICKS;
        this.taczaddon$latestAcceptedRefreshRequestId = -1L;
        this.taczaddon$latestSourceRevision = -1L;
        this.taczaddon$hasAcceptedSnapshot = false;
        this.taczaddon$externalDisplayStacks = List.of();
    }

    @Unique
    private void taczaddon$clearPendingRefresh() {
        this.taczaddon$pendingRefreshRequestId = -1L;
        this.taczaddon$refreshInFlight = false;
        this.taczaddon$pendingRefreshAgeTicks = 0;
    }
}
