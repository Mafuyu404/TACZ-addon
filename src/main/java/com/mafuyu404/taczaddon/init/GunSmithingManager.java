package com.mafuyu404.taczaddon.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class GunSmithingManager {
    public static final String UNLOCKED_ATTACHMENTS_TAG =
            "UnlockedAttachments";

    /*
     * Backward-compatibility map for guns created by the previous
     * CombinedItems implementation.
     *
     * New guns no longer depend on this cache.
     */
    private static final Map<
            ResourceLocation,
            Set<ResourceLocation>
            > LEGACY_CACHE = new HashMap<>();

    private GunSmithingManager() {
    }

    /**
     * Retained for old recipe-constructor calls and old gun data.
     */
    @Deprecated(forRemoval = false)
    public static synchronized void putCache(
            String itemIdText,
            List<String> attachmentIdTexts
    ) {
        ResourceLocation itemId =
                ResourceLocation.tryParse(itemIdText);

        if (itemId == null) {
            return;
        }

        Set<ResourceLocation> attachmentIds =
                LEGACY_CACHE.computeIfAbsent(
                        itemId,
                        ignored -> new LinkedHashSet<>()
                );

        for (String attachmentIdText :
                attachmentIdTexts) {
            ResourceLocation attachmentId =
                    ResourceLocation.tryParse(
                            attachmentIdText
                    );

            if (attachmentId != null) {
                attachmentIds.add(attachmentId);
            }
        }
    }

    public static synchronized void clearLegacyCache() {
        LEGACY_CACHE.clear();
    }

    /**
     * Writes actual attachment IDs to the gun.
     *
     * This removes the runtime dependency on recipe-constructor cache state.
     */
    public static void addUnlockedAttachments(
            CompoundTag customData,
            Collection<ResourceLocation> attachmentIds
    ) {
        ListTag storedIds;

        if (customData.contains(
                UNLOCKED_ATTACHMENTS_TAG,
                Tag.TAG_LIST
        )) {
            storedIds = customData.getList(
                    UNLOCKED_ATTACHMENTS_TAG,
                    Tag.TAG_STRING
            );
        } else {
            storedIds = new ListTag();
        }

        Set<String> existingIds = new LinkedHashSet<>();

        for (Tag tag : storedIds) {
            existingIds.add(tag.getAsString());
        }

        for (ResourceLocation attachmentId :
                attachmentIds) {
            if (attachmentId == null) {
                continue;
            }

            String serializedId = attachmentId.toString();

            if (existingIds.add(serializedId)) {
                storedIds.add(
                        StringTag.valueOf(serializedId)
                );
            }
        }

        customData.put(
                UNLOCKED_ATTACHMENTS_TAG,
                storedIds
        );
    }

    public static Set<ResourceLocation> getResult(
            ItemStack gunStack
    ) {
        if (gunStack.isEmpty()) {
            return Set.of();
        }

        CompoundTag customData =
                ItemStackData.getCustomDataCopy(gunStack);

        Set<ResourceLocation> result =
                new LinkedHashSet<>();

        readDirectAttachmentIds(customData, result);
        readLegacyCombinedItems(customData, result);

        return Set.copyOf(result);
    }

    private static void readDirectAttachmentIds(
            CompoundTag customData,
            Set<ResourceLocation> result
    ) {
        if (!customData.contains(
                UNLOCKED_ATTACHMENTS_TAG,
                Tag.TAG_LIST
        )) {
            return;
        }

        ListTag attachmentIds =
                customData.getList(
                        UNLOCKED_ATTACHMENTS_TAG,
                        Tag.TAG_STRING
                );

        for (Tag tag : attachmentIds) {
            ResourceLocation attachmentId =
                    ResourceLocation.tryParse(
                            tag.getAsString()
                    );

            if (attachmentId != null) {
                result.add(attachmentId);
            }
        }
    }

    private static synchronized void readLegacyCombinedItems(
            CompoundTag customData,
            Set<ResourceLocation> result
    ) {
        if (!customData.contains(
                "CombinedItems",
                Tag.TAG_LIST
        )) {
            return;
        }

        ListTag combinedItems =
                customData.getList(
                        "CombinedItems",
                        Tag.TAG_STRING
                );

        for (Tag tag : combinedItems) {
            ResourceLocation itemId =
                    ResourceLocation.tryParse(
                            tag.getAsString()
                    );

            if (itemId == null) {
                continue;
            }

            Set<ResourceLocation> legacyUnlocks =
                    LEGACY_CACHE.get(itemId);

            if (legacyUnlocks != null) {
                result.addAll(legacyUnlocks);
            }
        }
    }

    public static ResourceLocation getItemRegistryId(
            Item item
    ) {
        if (item == null) {
            return null;
        }

        return BuiltInRegistries.ITEM.getKey(item);
    }

    /**
     * Retained for source compatibility.
     */
    @Deprecated(forRemoval = false)
    public static String getItemRegistryName(Item item) {
        ResourceLocation itemId =
                getItemRegistryId(item);

        return itemId == null
                ? null
                : itemId.toString();
    }
}