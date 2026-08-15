package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.PlayerInventorySource;
import com.mafuyu404.taczaddon.init.crafting.RefitSourceLocator;
import com.mafuyu404.taczaddon.network.RefitExternalAttachmentInstallPacket;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Authoritative external-source attachment install.
 *
 * The client locator is only a hint. Every request re-resolves the currently
 * legal nearby source set from the server player position and verifies the
 * exact physical slot before any item is touched.
 */
public final class RefitExternalInstallService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RefitExternalInstallService() {
    }

    public static void handle(
            RefitExternalAttachmentInstallPacket request,
            @Nullable ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        if (!CommonConfig.enableNearbyContainerSources()
                || LiberateAttachmentService.isEnabled(player)) {
            reject(player);
            return;
        }

        ItemStack gunStack =
                LiberateAttachmentService.getCurrentRealGun(
                        player,
                        request.gunSlotIndex()
                );
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gunStack == null
                || gun == null
                || gun.hasAttachmentLock(gunStack)) {
            reject(player);
            return;
        }

        List<CraftingItemSource> sources =
                RefitSourceResolver.resolveExternalSources(player);
        Optional<CraftingItemSource> found =
                RefitSourceResolver.findSource(
                        sources,
                        request.locator()
                );
        if (found.isEmpty()) {
            reject(player);
            return;
        }

        CraftingItemSource source = found.get();
        try {
            installFromSource(
                    player,
                    gunStack,
                    gun,
                    source,
                    request
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit external install failed for player {} source {}",
                    player.getGameProfile().getName(),
                    source.key(),
                    exception
            );
            reject(player);
        }
    }

    private static void installFromSource(
            ServerPlayer player,
            ItemStack gunStack,
            IGun gun,
            CraftingItemSource source,
            RefitExternalAttachmentInstallPacket request
    ) {
        RefitSourceLocator locator = request.locator();
        if (!source.isValid(player)
                || locator.slot() < 0
                || locator.slot() >= source.slotCount()) {
            reject(player);
            return;
        }

        ItemStack current = source.getStackInSlot(locator.slot());
        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(current);
        if (attachment == null) {
            reject(player);
            return;
        }

        ResourceLocation actualId =
                attachment.getAttachmentId(current);
        AttachmentType actualType = attachment.getType(current);
        if (!LiberateAttachmentService.isValidCandidate(
                request.expectedAttachmentId(),
                actualId,
                request.expectedType(),
                actualType,
                gun.hasAttachmentLock(gunStack),
                gun.allowAttachment(gunStack, current)
        )) {
            reject(player);
            return;
        }

        ItemStack simulated = source.extractItem(
                locator.slot(),
                1,
                true
        );
        if (!matchesExtracted(simulated, current)) {
            reject(player);
            return;
        }

        ItemStack extracted = source.extractItem(
                locator.slot(),
                1,
                false
        );
        if (!matchesExtracted(extracted, current)) {
            if (!extracted.isEmpty()) {
                rollbackExtraction(
                        player,
                        source,
                        locator.slot(),
                        extracted
                );
            }
            reject(player);
            return;
        }

        ItemStack replaced = gun.getAttachment(
                gunStack,
                actualType
        );

        try {
            gun.installAttachment(gunStack, extracted);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Gun refit mutation failed after external extraction "
                            + "for player {}; restoring source slot {}",
                    player.getGameProfile().getName(),
                    locator.slot(),
                    exception
            );
            rollbackExtraction(
                    player,
                    source,
                    locator.slot(),
                    extracted
            );
            reject(player);
            return;
        }

        AttachmentPropertyManager.postChangeEvent(player, gunStack);
        if (actualType == AttachmentType.EXTENDED_MAG) {
            gun.dropAllAmmo(player, gunStack);
        }

        returnReplacedAttachment(player, replaced);
        synchronize(player, source);
        LiberateAttachmentService.refreshRefitScreen(player);
    }

    private static boolean matchesExtracted(
            ItemStack extracted,
            ItemStack expected
    ) {
        return !extracted.isEmpty()
                && extracted.getCount() == 1
                && ItemStack.isSameItemSameTags(extracted, expected);
    }

    private static void returnReplacedAttachment(
            ServerPlayer player,
            ItemStack replaced
    ) {
        if (replaced.isEmpty()) {
            return;
        }

        ItemStack remainder = replaced.copy();
        boolean added = player.getInventory().add(remainder);
        if (!added) {
            player.drop(remainder, false);
        }
    }

    private static void rollbackExtraction(
            ServerPlayer player,
            CraftingItemSource source,
            int originalSlot,
            ItemStack extracted
    ) {
        boolean fullyRestored = true;
        ItemStack remainder = insertSafely(
                source,
                originalSlot,
                extracted,
                "refit-original-slot"
        );

        if (!remainder.isEmpty()) {
            remainder = insertIntoOtherSlots(
                    source,
                    originalSlot,
                    remainder
            );
        }

        if (!remainder.isEmpty()) {
            PlayerInventorySource playerSource =
                    new PlayerInventorySource(player);
            remainder = insertIntoOtherSlots(
                    playerSource,
                    -1,
                    remainder
            );
        }

        if (!remainder.isEmpty()) {
            fullyRestored = false;
            LOGGER.error(
                    "Refit rollback could not restore item {} x{}; source {} "
                            + "slot {} player {}",
                    remainder.getHoverName().getString(),
                    remainder.getCount(),
                    source.key(),
                    originalSlot,
                    player.getGameProfile().getName()
            );
            player.drop(remainder, false);
        }

        if (!fullyRestored) {
            LOGGER.warn(
                    "Refit rollback only partially compensated source {}",
                    source.key()
            );
        }
        synchronize(player, source);
    }

    private static ItemStack insertSafely(
            CraftingItemSource source,
            int slot,
            ItemStack stack,
            String operation
    ) {
        try {
            return source.insertItem(slot, stack, false);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit rollback {} failed for source {} slot {}",
                    operation,
                    source.key(),
                    slot,
                    exception
            );
            return stack;
        }
    }

    private static ItemStack insertIntoOtherSlots(
            CraftingItemSource source,
            int excludedSlot,
            ItemStack stack
    ) {
        ItemStack remainder = stack;
        int slots;
        try {
            slots = source.slotCount();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit rollback could not read slot count for {}",
                    source.key(),
                    exception
            );
            return remainder;
        }

        for (int slot = 0;
             slot < slots && !remainder.isEmpty();
             slot++) {
            if (slot == excludedSlot) {
                continue;
            }
            remainder = insertSafely(
                    source,
                    slot,
                    remainder,
                    "refit-other-slot"
            );
        }
        return remainder;
    }

    private static void synchronize(
            ServerPlayer player,
            CraftingItemSource source
    ) {
        try {
            source.markChanged();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit source markChanged failed for {}",
                    source.key(),
                    exception
            );
        }
        try {
            source.synchronize(player);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit source synchronize failed for {}",
                    source.key(),
                    exception
            );
        }
        try {
            player.inventoryMenu.broadcastChanges();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Refit player inventory sync failed",
                    exception
            );
        }
    }

    private static void reject(ServerPlayer player) {
        LiberateAttachmentService.refreshRefitScreen(player);
    }
}
