package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.mafuyu404.taczaddon.init.crafting.NearbyInventorySourceResolver;
import com.mafuyu404.taczaddon.init.crafting.RefitSourceLocator;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-side refit source layer. It uses the same generic nearby-container
 * scanner as Gunsmith crafting, but returns only physical attachment
 * candidates with enough identity for a later authoritative install.
 */
public final class RefitSourceResolver {
    public static final int MAX_EXTERNAL_CANDIDATES = 256;

    private static final Logger LOGGER = LogUtils.getLogger();

    private RefitSourceResolver() {
    }

    public static List<CraftingItemSource> resolveExternalSources(
            ServerPlayer player
    ) {
        if (!CommonConfig.enableNearbyContainerSources()) {
            return List.of();
        }

        BlockPos anchor = player.blockPosition();
        return NearbyInventorySourceResolver.resolve(
                player,
                anchor,
                CommonConfig.getNearbyContainerScanRadius(),
                1
        );
    }

    public static List<RefitExternalCandidate> resolveExternalCandidates(
            ServerPlayer player
    ) {
        ItemStack heldGun = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(heldGun);
        if (gun == null) {
            return List.of();
        }

        ArrayList<RefitExternalCandidate> candidates =
                new ArrayList<>();

        for (CraftingItemSource source
                : resolveExternalSources(player)) {
            try {
                collectCandidatesFromSource(
                        player,
                        heldGun,
                        gun,
                        source,
                        candidates
                );
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Skipping unreadable refit source {}",
                        source.key(),
                        exception
                );
            }
        }

        return candidates.size() > MAX_EXTERNAL_CANDIDATES
                ? List.copyOf(
                candidates.subList(
                        0,
                        MAX_EXTERNAL_CANDIDATES
                )
        )
                : List.copyOf(candidates);
    }

    private static void collectCandidatesFromSource(
            ServerPlayer player,
            ItemStack heldGun,
            IGun gun,
            CraftingItemSource source,
            List<RefitExternalCandidate> candidates
    ) {
        if (!source.isValid(player)) {
            return;
        }

        if (!(source.key()
                instanceof CraftingSourceKey.BlockEntity blockKey)) {
            return;
        }

        int slots = source.slotCount();
        for (int slot = 0; slot < slots; slot++) {
            if (candidates.size() >= MAX_EXTERNAL_CANDIDATES) {
                return;
            }

            ItemStack stack = source.getStackInSlot(slot);
            IAttachment attachment =
                    IAttachment.getIAttachmentOrNull(stack);
            if (attachment == null) {
                continue;
            }

            ResourceLocation attachmentId =
                    attachment.getAttachmentId(stack);
            AttachmentType type = attachment.getType(stack);
            if (attachmentId == null
                    || type == null
                    || type == AttachmentType.NONE
                    || !gun.allowAttachment(heldGun, stack)) {
                continue;
            }

            candidates.add(new RefitExternalCandidate(
                    attachmentId,
                    type,
                    RefitSourceLocator.fromBlockSource(
                            blockKey,
                            slot
                    ),
                    stack.copy()
            ));
        }
    }

    public static Optional<CraftingItemSource> findSource(
            List<CraftingItemSource> sources,
            RefitSourceLocator locator
    ) {
        for (CraftingItemSource source : sources) {
            if (source.key()
                    instanceof CraftingSourceKey.BlockEntity blockKey
                    && blockKey.dimension().equals(locator.dimension())
                    && blockKey.pos().equals(locator.pos())) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }

    public record RefitExternalCandidate(
            ResourceLocation attachmentId,
            AttachmentType type,
            RefitSourceLocator locator,
            ItemStack displayStack
    ) {
        public RefitExternalCandidate {
            displayStack = displayStack.copy();
        }

        public RefitExternalCandidate copy() {
            return new RefitExternalCandidate(
                    this.attachmentId,
                    this.type,
                    this.locator,
                    this.displayStack
            );
        }
    }
}
