package com.mafuyu404.taczaddon.common;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-session-only browse memory for TaCZ gunsmith tables.
 *
 * State is keyed by the data-driven table definition id. It is not persisted
 * to disk and never participates in server crafting authorization.
 */
public final class BetterGunSmithTable {
    private static final int MAX_REMEMBERED_TABLES = 64;

    private static final Map<ResourceLocation, BrowseState> BROWSE_STATES =
            new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<ResourceLocation, BrowseState> eldest
                ) {
                    return size() > MAX_REMEMBERED_TABLES;
                }
            };

    private BetterGunSmithTable() {
    }

    public record BrowseState(
            @Nullable ResourceLocation selectedType,
            @Nullable ResourceLocation selectedRecipeId,
            int typePage,
            int indexPage
    ) {
        public BrowseState {
            typePage = Math.max(0, typePage);
            indexPage = Math.max(0, indexPage);
        }
    }

    public static synchronized void saveBrowseState(
            ResourceLocation tableDefinitionId,
            @Nullable ResourceLocation selectedType,
            @Nullable ResourceLocation selectedRecipeId,
            int typePage,
            int indexPage
    ) {
        if (tableDefinitionId == null) {
            return;
        }

        BROWSE_STATES.put(
                tableDefinitionId,
                new BrowseState(
                        selectedType,
                        selectedRecipeId,
                        typePage,
                        indexPage
                )
        );
    }

    public static synchronized Optional<BrowseState> getBrowseState(
            ResourceLocation tableDefinitionId
    ) {
        if (tableDefinitionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                BROWSE_STATES.get(tableDefinitionId)
        );
    }

    public static synchronized void clearBrowseState(
            ResourceLocation tableDefinitionId
    ) {
        if (tableDefinitionId != null) {
            BROWSE_STATES.remove(tableDefinitionId);
        }
    }

    public static synchronized void clearAllBrowseStates() {
        BROWSE_STATES.clear();
    }
}
