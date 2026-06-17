package com.mafuyu404.taczaddon.common;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BetterGunSmithTable {

    /**
     * Client-session browse state.
     *
     * This is intentionally not saved to config/disk.
     * It only remembers the last position while the client is running.
     */
    public record BrowseState(
            ResourceLocation tableId,
            ResourceLocation selectedType,
            ResourceLocation selectedRecipeId,
            int typePage,
            int indexPage,
            int attachmentPropIndex
    ) {}

    private static final Map<ResourceLocation, BrowseState> BROWSE_STATES =
            new HashMap<>();

    public static void saveBrowseState(
            ResourceLocation tableId,
            ResourceLocation selectedType,
            ResourceLocation selectedRecipeId,
            int typePage,
            int indexPage,
            int attachmentPropIndex
    ) {
        if (tableId == null) {
            return;
        }

        BROWSE_STATES.put(
                tableId,
                new BrowseState(
                        tableId,
                        selectedType,
                        selectedRecipeId,
                        Math.max(0, typePage),
                        Math.max(0, indexPage),
                        Math.max(0, attachmentPropIndex)
                )
        );
    }

    public static Optional<BrowseState> getBrowseState(
            ResourceLocation tableId
    ) {
        if (tableId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(BROWSE_STATES.get(tableId));
    }

    public static void clearBrowseState() {
        BROWSE_STATES.clear();
    }

    public static boolean allowAttachment(ItemStack gunItem, ResourceLocation attachmentId) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        ResourceLocation gunId = null;
        if (iGun != null) {
            gunId = iGun.getGunId(gunItem);
        }
        return AllowAttachmentTagMatcher.match(gunId, attachmentId);
    }

    public static boolean allowAmmo(ItemStack gunItem, ResourceLocation ammoId) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        ResourceLocation gunId = null;
        if (iGun != null) {
            gunId = iGun.getGunId(gunItem);
        }
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId))
                .orElse(false);
    }

    public static boolean isHoldingGun(Player player) {
        if (IGun.getIGunOrNull(player.getMainHandItem()) != null) return true;
        return IGun.getIGunOrNull(player.getOffhandItem()) != null;
    }

    public static ItemStack getHoldingGun(Player player) {
        IGun main = IGun.getIGunOrNull(player.getMainHandItem());
        IGun off = IGun.getIGunOrNull(player.getOffhandItem());

        if (main != null) return player.getMainHandItem();
        if (off != null) return player.getOffhandItem();

        return null;
    }

    public static String getTranslationKey(Component component) {
        ComponentContents contents = component.getContents();

        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }

        return null;
    }
}