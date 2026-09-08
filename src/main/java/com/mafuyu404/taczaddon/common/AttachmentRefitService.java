package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.RefreshRefitScreenPacket;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Server-authoritative attachment refit transaction service.
 *
 * Both {@link com.mafuyu404.taczaddon.network.VirtualAttachmentRefitPacket}
 * and the mixin that takes over TaCZ's
 * {@code ClientMessageUnloadAttachment} delegate here, so there is exactly
 * one ownership/transaction implementation.
 *
 * <p>Ownership model:
 * <ul>
 *     <li>PHYSICAL attachments (no explicit virtual marker) are returned to
 *     the player's main inventory on replacement/unload. The return is a
 *     fail-safe {@link MainInventoryTransaction}: simulated preflight first,
 *     and any unexpected insertion failure rolls the gun and the main
 *     inventory back instead of losing the item.</li>
 *     <li>VIRTUAL attachments carry an explicit CUSTOM_DATA marker and are
 *     retired (discarded) on replacement/unload. They never enter any real
 *     inventory and never require inventory capacity.</li>
 * </ul>
 *
 * <p>Side effects (postChangeEvent, EXTENDED_MAG ammo drop, inventory sync,
 * screen refresh) only run after the whole transaction has committed.
 */
public final class AttachmentRefitService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** sourceSlot value used by the refit screen for virtual attachments. */
    public static final int VIRTUAL_SOURCE_SLOT = -1;

    private static final int HOTBAR_SIZE = 9;

    private static final String NO_SPACE_KEY =
            "gui.tacz.gun_refit.unload.no_space";

    private AttachmentRefitService() {
    }

    public enum InstallResult {
        SUCCESS,
        REJECTED,
        NO_SPACE,
        INTERNAL_FAILURE
    }

    /** Extends the existing ownership model with a reversible nearby source. */
    public static InstallResult installExternal(ServerPlayer player, int gunSlot,
            com.mafuyu404.taczaddon.init.NearbyInventorySourceResolver.Source source, int sourceSlot,
            ResourceLocation expectedId, AttachmentType expectedType) {
        Inventory inventory = player.getInventory();
        if (!RefitSourceResolver.canUseSources(player) || !isValidGunSlot(inventory, gunSlot) || !source.isValid()
                || !(source.handler() instanceof net.neoforged.neoforge.items.IItemHandlerModifiable handler)
                || sourceSlot < 0 || sourceSlot >= handler.getSlots()) return InstallResult.REJECTED;
        ItemStack gunStack = inventory.getItem(gunSlot);
        IGun gun = IGun.getIGunOrNull(gunStack);
        ItemStack current = handler.getStackInSlot(sourceSlot);
        IAttachment attachment = IAttachment.getIAttachmentOrNull(current);
        if (gun == null || gun.hasAttachmentLock(gunStack) || attachment == null
                || !expectedId.equals(attachment.getAttachmentId(current)) || expectedType != attachment.getType(current)
                || !gun.allowAttachment(gunStack, current)) return InstallResult.REJECTED;
        ExternalSourceExtraction extraction = new ExternalSourceExtraction(handler, sourceSlot);
        if (!extraction.simulateOne()) return InstallResult.REJECTED;
        ItemStack oldAttachment = gun.getAttachment(player.registryAccess(), gunStack, expectedType).copy();
        ItemStack physicalReturn = VirtualAttachmentData.isVirtual(oldAttachment) ? ItemStack.EMPTY : oldAttachment;
        MainInventoryTransaction transaction = MainInventoryTransaction.begin(inventory);
        try {
            // Capacity failure must precede extraction, even when that would free a source slot.
            if (!transaction.canFullyInsert(physicalReturn)) {
                transaction.close();
                sendNoSpace(player);
                return InstallResult.NO_SPACE;
            }
            ItemStack extracted = extraction.extractOne();
            gun.installAttachment(player.registryAccess(), gunStack, extracted);
            if (!transaction.commitInsert(physicalReturn).isEmpty()) {
                throw new IllegalStateException("Physical attachment return failed after preflight");
            }
            transaction.close();
        } catch (RuntimeException exception) {
            logInternalFailure("external-install", expectedId, exception);
            try {
                extraction.rollback();
                source.markChanged();
            } catch (RuntimeException recoveryException) {
                LOGGER.error("CRITICAL: external source rollback failed at {} slot {}", source.pos(), sourceSlot, recoveryException);
            }
            handleRollbackOutcome(transaction, player, inventory, "external-install", expectedId);
            return InstallResult.INTERNAL_FAILURE;
        }
        source.markChanged();
        postChange(player, gunStack, expectedType, inventory);
        return InstallResult.SUCCESS;
    }

    public enum UnloadResult {
        SUCCESS,
        REJECTED,
        NO_SPACE,
        INTERNAL_FAILURE
    }

    /**
     * Handles one install/replacement transaction. The transaction covers the
     * four source combinations:
     *
     * <pre>
     * old \ new       VIRTUAL            PHYSICAL
     * VIRTUAL         install+discard    install+discard, consume source
     * NONE/PHYSICAL   fail-safe return   TaCZ native swap semantics
     * </pre>
     *
     * <p>For PHYSICAL -> VIRTUAL the old physical attachment is preflighted
     * into the main inventory before the gun is touched. If the real insert
     * ever fails afterwards, the gun attachment and the main inventory are
     * rolled back to their previous state and no side effects run.
     */
    public static InstallResult install(
            ServerPlayer player,
            int sourceSlot,
            int gunSlot,
            ResourceLocation attachmentId
    ) {
        if (player == null || attachmentId == null) {
            return InstallResult.REJECTED;
        }

        Inventory inventory = player.getInventory();

        if (!isValidGunSlot(inventory, gunSlot)) {
            return InstallResult.REJECTED;
        }

        ItemStack gunStack = inventory.getItem(gunSlot);

        IGun gun = IGun.getIGunOrNull(gunStack);

        if (gun == null || gun.hasAttachmentLock(gunStack)) {
            return InstallResult.REJECTED;
        }

        boolean newVirtual = sourceSlot == VIRTUAL_SOURCE_SLOT;

        ItemStack attachmentStack;

        if (newVirtual) {
            Optional<ItemStack> virtualStack =
                    buildValidatedVirtualAttachment(
                            player,
                            gunStack,
                            attachmentId
                    );

            if (virtualStack.isEmpty()) {
                return InstallResult.REJECTED;
            }

            attachmentStack = virtualStack.get();
        } else {
            Optional<ItemStack> physicalStack =
                    getValidatedPhysicalAttachment(
                            inventory,
                            sourceSlot,
                            attachmentId
                    );

            if (physicalStack.isEmpty()) {
                return InstallResult.REJECTED;
            }

            attachmentStack = physicalStack.get();
        }

        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(attachmentStack);

        if (attachment == null) {
            return InstallResult.REJECTED;
        }

        ResourceLocation actualAttachmentId =
                attachment.getAttachmentId(attachmentStack);

        if (!attachmentId.equals(actualAttachmentId)) {
            return InstallResult.REJECTED;
        }

        if (!gun.allowAttachment(gunStack, attachmentStack)) {
            return InstallResult.REJECTED;
        }

        AttachmentType actualType =
                attachment.getType(attachmentStack);

        if (actualType == AttachmentType.NONE) {
            return InstallResult.REJECTED;
        }

        var registryAccess = player.registryAccess();

        ItemStack oldAttachment =
                gun.getAttachment(
                        registryAccess,
                        gunStack,
                        actualType
                ).copy();

        boolean oldVirtual =
                VirtualAttachmentData.isVirtual(oldAttachment);

        if (!newVirtual) {
            /*
             * PHYSICAL install: TaCZ native source-slot swap semantics.
             *
             * The old attachment replaces the consumed source stack (or the
             * source slot is emptied when the old one was virtual). A source
             * stack that unexpectedly carries the virtual marker is never
             * "converted" to physical: the marker stays and the server keeps
             * treating it as virtual provenance.
             *
             * Order is install-first, setItem-second, so an install exception
             * cannot leave the source slot mutated. This path intentionally
             * follows TaCZ native swap semantics and is NOT covered by the
             * defensive MainInventoryTransaction rollback used by the
             * physical-return paths (a source-slot swap cannot partially
             * insert into arbitrary inventory slots).
             */
            gun.installAttachment(
                    registryAccess,
                    gunStack,
                    attachmentStack
            );

            inventory.setItem(
                    sourceSlot,
                    oldVirtual ? ItemStack.EMPTY : oldAttachment
            );

            postChange(
                    player,
                    gunStack,
                    actualType,
                    inventory
            );

            return InstallResult.SUCCESS;
        }

        /*
         * VIRTUAL install. Old virtual attachments are simply retired.
         * Old physical attachments go through a fail-safe main-inventory
         * return transaction.
         */
        if (oldVirtual || oldAttachment.isEmpty()) {
            gun.installAttachment(
                    registryAccess,
                    gunStack,
                    attachmentStack
            );

            postChange(
                    player,
                    gunStack,
                    actualType,
                    inventory
            );

            return InstallResult.SUCCESS;
        }

        MainInventoryTransaction transaction =
                MainInventoryTransaction.begin(inventory);

        try {
            if (!transaction.canFullyInsert(oldAttachment)) {
                transaction.close();
                sendNoSpace(player);
                return InstallResult.NO_SPACE;
            }

            gun.installAttachment(
                    registryAccess,
                    gunStack,
                    attachmentStack
            );

            ItemStack remainder =
                    transaction.commitInsert(oldAttachment);

            if (!remainder.isEmpty()) {
                logInternalFailure(
                        "install",
                        attachmentId,
                        remainder
                );
                handleRollbackOutcome(
                        transaction,
                        player,
                        inventory,
                        "install",
                        attachmentId
                );
                return InstallResult.INTERNAL_FAILURE;
            }

            transaction.close();
        } catch (RuntimeException exception) {
            logInternalFailure(
                    "install",
                    attachmentId,
                    exception
            );
            handleRollbackOutcome(
                    transaction,
                    player,
                    inventory,
                    "install",
                    attachmentId
            );
            return InstallResult.INTERNAL_FAILURE;
        }

        postChange(
                player,
                gunStack,
                actualType,
                inventory
        );

        return InstallResult.SUCCESS;
    }

    /**
     * Handles one unload transaction.
     *
     * <p>VIRTUAL attachments are simply unloaded and retired; inventory
     * capacity is irrelevant. PHYSICAL attachments are preflighted into the
     * main inventory before the gun is modified, and any unexpected insertion
     * failure rolls both back.
     */
    public static UnloadResult unload(
            ServerPlayer player,
            int gunSlot,
            AttachmentType type
    ) {
        if (player == null
                || type == null
                || type == AttachmentType.NONE) {
            return UnloadResult.REJECTED;
        }

        Inventory inventory = player.getInventory();

        if (!isValidGunSlot(inventory, gunSlot)) {
            return UnloadResult.REJECTED;
        }

        ItemStack gunStack = inventory.getItem(gunSlot);

        IGun gun = IGun.getIGunOrNull(gunStack);

        if (gun == null || gun.hasAttachmentLock(gunStack)) {
            return UnloadResult.REJECTED;
        }

        var registryAccess = player.registryAccess();

        ItemStack attachedStack =
                gun.getAttachment(
                        registryAccess,
                        gunStack,
                        type
                );

        if (attachedStack.isEmpty()) {
            return UnloadResult.REJECTED;
        }

        boolean virtual =
                VirtualAttachmentData.isVirtual(attachedStack);

        if (virtual) {
            gun.unloadAttachment(
                    registryAccess,
                    gunStack,
                    type
            );

            postChange(
                    player,
                    gunStack,
                    type,
                    inventory
            );

            return UnloadResult.SUCCESS;
        }

        MainInventoryTransaction transaction =
                MainInventoryTransaction.begin(inventory);

        try {
            if (!transaction.canFullyInsert(attachedStack)) {
                transaction.close();
                sendNoSpace(player);
                return UnloadResult.NO_SPACE;
            }

            gun.unloadAttachment(
                    registryAccess,
                    gunStack,
                    type
            );

            ItemStack remainder =
                    transaction.commitInsert(attachedStack);

            if (!remainder.isEmpty()) {
                logInternalFailure(
                        "unload",
                        type,
                        remainder
                );
                handleRollbackOutcome(
                        transaction,
                        player,
                        inventory,
                        "unload",
                        type
                );
                return UnloadResult.INTERNAL_FAILURE;
            }

            transaction.close();
        } catch (RuntimeException exception) {
            logInternalFailure(
                    "unload",
                    type,
                    exception
            );
            handleRollbackOutcome(
                    transaction,
                    player,
                    inventory,
                    "unload",
                    type
            );
            return UnloadResult.INTERNAL_FAILURE;
        }

        postChange(
                player,
                gunStack,
                type,
                inventory
        );

        return UnloadResult.SUCCESS;
    }

    /**
     * The refit screen only ever operates on the selected main-hand slot.
     */
    private static boolean isValidGunSlot(
            Inventory inventory,
            int gunSlot
    ) {
        return gunSlot >= 0
                && gunSlot < HOTBAR_SIZE
                && gunSlot == inventory.selected;
    }

    /**
     * Server-side virtual permission check, then canonical stack construction
     * and marking. The client never supplies the ItemStack or the marker.
     */
    private static Optional<ItemStack> buildValidatedVirtualAttachment(
            ServerPlayer player,
            ItemStack gunStack,
            ResourceLocation attachmentId
    ) {
        if (!LiberateAttachment.canUseVirtualAttachment(
                player,
                gunStack,
                attachmentId
        )) {
            return Optional.empty();
        }

        return LiberateAttachment
                .findAttachmentStack(attachmentId)
                .map(VirtualAttachmentData::markVirtual);
    }

    /**
     * Physical source slots are restricted to the main inventory
     * (Inventory.items, 0..35). Armor/offhand/backpack slots are never valid
     * sources, matching the "physical return stays in the main inventory"
     * storage domain.
     */
    private static Optional<ItemStack> getValidatedPhysicalAttachment(
            Inventory inventory,
            int sourceSlot,
            ResourceLocation expectedId
    ) {
        if (sourceSlot < 0
                || sourceSlot >= inventory.items.size()) {
            return Optional.empty();
        }

        if (sourceSlot == inventory.selected) {
            return Optional.empty();
        }

        ItemStack stack = inventory.getItem(sourceSlot);

        if (stack.isEmpty()) {
            return Optional.empty();
        }

        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(stack);

        if (attachment == null) {
            return Optional.empty();
        }

        ResourceLocation actualId =
                attachment.getAttachmentId(stack);

        if (!expectedId.equals(actualId)) {
            return Optional.empty();
        }

        return Optional.of(stack);
    }

    /**
     * Shared TaCZ side effects after a successful install/unload. Only
     * invoked once the transaction has fully committed.
     */
    private static void postChange(
            ServerPlayer player,
            ItemStack gunStack,
            AttachmentType type,
            Inventory inventory
    ) {
        AttachmentPropertyManager.postChangeEvent(
                player,
                gunStack
        );

        if (type == AttachmentType.EXTENDED_MAG) {
            IGun gun = IGun.getIGunOrNull(gunStack);

            if (gun != null) {
                gun.dropAllAmmo(player, gunStack);
            }
        }

        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();

        NetworkHandler.sendToClient(
                player,
                new RefreshRefitScreenPacket(true)
        );
    }

    private static void sendNoSpace(ServerPlayer player) {
        player.sendSystemMessage(
                Component.translatable(NO_SPACE_KEY)
        );
    }

    /**
     * Attempts a transaction rollback and logs the true recovery outcome.
     *
     * <p>The {@link MainInventoryTransaction} snapshot is the only rollback
     * authority: it restores all 36 main slots, including the gun slot with
     * its complete ItemStack. No stale gun reference is touched afterwards.
     *
     * <p>Returns true only when rollback completed. On rollback failure the
     * situation is logged as CRITICAL (never as "nothing lost"), and the
     * caller must not pretend recovery succeeded.
     */
    private static boolean safeRollback(
            MainInventoryTransaction transaction,
            String operation,
            Object detail
    ) {
        try {
            transaction.rollback();

            LOGGER.error(
                    "[taczaddon] Transaction failed during {} (detail={}); "
                            + "rollback completed. Main inventory (including "
                            + "the gun slot) restored to the pre-transaction "
                            + "state. Nothing was dropped or duplicated.",
                    operation,
                    detail
            );

            return true;
        } catch (RuntimeException rollbackException) {
            LOGGER.error(
                    "[taczaddon] CRITICAL: transaction failed during {} "
                            + "(detail={}) and rollback itself also failed. "
                            + "Player inventory may require manual "
                            + "inspection.",
                    operation,
                    detail,
                    rollbackException
            );

            return false;
        }
    }

    /**
     * Unified recovery handling shared by the install and unload
     * transactions.
     *
     * <p>On rollback success the authoritative container state is synced and
     * the refit screen is refreshed. On rollback failure a best-effort
     * full-state sync is still attempted so the client cannot keep a stale or
     * predicted view of a possibly partially-mutated server state.
     */
    private static void handleRollbackOutcome(
            MainInventoryTransaction transaction,
            ServerPlayer player,
            Inventory inventory,
            String operation,
            Object detail
    ) {
        boolean rollbackSucceeded =
                safeRollback(transaction, operation, detail);

        if (rollbackSucceeded) {
            syncAfterRollback(player, inventory);
        } else {
            bestEffortSyncAfterRecoveryFailure(
                    player,
                    inventory,
                    operation,
                    detail
            );
        }
    }

    /**
     * Synchronizes a successfully rolled-back transaction to the client.
     *
     * <p>Only called for ROLLBACK_SUCCESS; see
     * {@link #bestEffortSyncAfterRecoveryFailure(ServerPlayer, Inventory,
     * String, Object)} for the rollback-failure path.
     *
     * <p>Order: container sync first, then the refit-screen refresh, so the
     * client re-inits its GUI from the restored server state instead of a
     * stale intermediate one. No success side effects
     * ({@code postChangeEvent} / ammo drop) are executed here: the gun is
     * back in its old state and must not be treated as a successful
     * attachment transaction.
     */
    private static void syncAfterRollback(
            ServerPlayer player,
            Inventory inventory
    ) {
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();

        NetworkHandler.sendToClient(
                player,
                new RefreshRefitScreenPacket(true)
        );
    }

    /**
     * Best-effort authoritative sync after a ROLLBACK FAILURE.
     *
     * <p>The server is the only authority at this point, but the rollback may
     * have failed or partially restored state. A full-state container sync
     * ({@code broadcastFullState()}, available in MC 1.21.1) is preferred
     * over a change-diff sync so the client cannot keep a stale/predicted
     * view. Every step is guarded so a third-layer recovery exception cannot
     * escape the network task; the transaction result stays
     * {@code INTERNAL_FAILURE}.
     *
     * <p>The refit screen is only refreshed if the selected slot still holds
     * a gun, so the client-side attachment cache refresh never runs against a
     * possibly corrupted/half-restored gun state.
     */
    private static void bestEffortSyncAfterRecoveryFailure(
            ServerPlayer player,
            Inventory inventory,
            String operation,
            Object detail
    ) {
        try {
            inventory.setChanged();
            player.inventoryMenu.broadcastFullState();
        } catch (RuntimeException syncException) {
            LOGGER.error(
                    "[taczaddon] CRITICAL: recovery sync also failed after "
                            + "a failed rollback during {} (detail={}). The "
                            + "client may not reflect the server inventory "
                            + "state.",
                    operation,
                    detail,
                    syncException
            );
            return;
        }

        try {
            if (IGun.getIGunOrNull(
                    inventory.getSelected()
            ) != null) {
                NetworkHandler.sendToClient(
                        player,
                        new RefreshRefitScreenPacket(true)
                );
            }
        } catch (RuntimeException refreshException) {
            LOGGER.error(
                    "[taczaddon] CRITICAL: refit screen refresh also failed "
                            + "after a failed rollback during {} (detail={}).",
                    operation,
                    detail,
                    refreshException
            );
        }
    }

    private static void logInternalFailure(
            String operation,
            Object detail,
            ItemStack remainder
    ) {
        LOGGER.error(
                "[taczaddon] Internal transaction failure during {} "
                        + "(detail={}, remainder={}).",
                operation,
                detail,
                remainder.toString()
        );
    }

    private static void logInternalFailure(
            String operation,
            Object detail,
            RuntimeException exception
    ) {
        LOGGER.error(
                "[taczaddon] Internal transaction failure during {} "
                        + "(detail={}).",
                operation,
                detail,
                exception
        );
    }
}
