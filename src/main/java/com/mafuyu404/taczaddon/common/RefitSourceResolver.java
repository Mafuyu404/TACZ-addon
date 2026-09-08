package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.*;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class RefitSourceResolver {
    public static final int MAX_EXTERNAL_CANDIDATES = 256;
    private RefitSourceResolver() {}
    public record ExternalCandidate(RefitSourceLocator locator, ResourceLocation attachmentId, AttachmentType type, ItemStack displayStack) {}
    public static boolean canUseSources(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu && Config.enableNearbyContainerSources()
                && !LiberateAttachment.isLiberated(player)
                && IGun.getIGunOrNull(player.getMainHandItem()) != null;
    }
    public static List<ExternalCandidate> resolveExternalCandidates(ServerPlayer player) {
        if (!canUseSources(player)) return List.of();
        ItemStack gunStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun.hasAttachmentLock(gunStack)) return List.of();
        List<ExternalCandidate> result = new ArrayList<>();
        for (var source : NearbyInventorySourceResolver.resolve(player, player.blockPosition(), Config.getContainerScanRadius(), 1)) {
            try {
                var handler = source.handler();
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
                    if (attachment == null) continue;
                    ResourceLocation id = attachment.getAttachmentId(stack);
                    AttachmentType type = attachment.getType(stack);
                    if (id == null || type == null || type == AttachmentType.NONE || !gun.allowAttachment(gunStack, stack)) continue;
                    result.add(new ExternalCandidate(new RefitSourceLocator(player.level().dimension().location(), source.pos(), source.kind(), slot),
                            id, type, stack.copy()));
                    if (result.size() >= MAX_EXTERNAL_CANDIDATES) return List.copyOf(result);
                }
            } catch (RuntimeException exception) {
                com.mojang.logging.LogUtils.getLogger().warn("Unreadable refit source {}", source.pos(), exception);
            }
        }
        return List.copyOf(result);
    }
}
