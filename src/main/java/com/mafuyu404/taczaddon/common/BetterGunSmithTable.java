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

import java.util.*;

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

    private static BrowseState browseState = null;

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

        browseState = new BrowseState(
                tableId,
                selectedType,
                selectedRecipeId,
                Math.max(0, typePage),
                Math.max(0, indexPage),
                Math.max(0, attachmentPropIndex)
        );
    }

    public static Optional<BrowseState> getBrowseState(ResourceLocation tableId) {
        if (tableId == null || browseState == null) {
            return Optional.empty();
        }

        if (browseState.tableId() != null && !tableId.equals(browseState.tableId())) {
            return Optional.empty();
        }

        return Optional.of(browseState);
    }

    public static void clearBrowseState() {
        browseState = null;
    }

    /**
     * Keep this as a harmless compatibility no-op if the old mixin hook still exists.
     * The old implementation was the bug: it wrote ids during classifyRecipes but never restored them.
     */
    @Deprecated
    public static ResourceLocation storeRecipeId(ResourceLocation id) {
        return id;
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