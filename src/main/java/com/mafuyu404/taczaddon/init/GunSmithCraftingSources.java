package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.mafuyu404.taczaddon.init.crafting.NearbyInventorySourceResolver;
import com.mafuyu404.taczaddon.init.crafting.PlayerInventorySource;
import com.mafuyu404.taczaddon.init.crafting.WorkbenchAnchor;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.*;

public final class GunSmithCraftingSources {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int MAX_EXTERNAL_STACKS = 256;

    private GunSmithCraftingSources() {
    }

    public static ResolvedSources resolve(
            ServerPlayer player,
            GunSmithCraftingSessionManager.GunSmithCraftingSession session
    ) {
        return resolve(
                player,
                new WorkbenchAnchor(
                        session.getDimension(),
                        session.getTablePos()
                ),
                session
        );
    }

    public static ResolvedSources resolve(
            ServerPlayer player,
            WorkbenchAnchor anchor
    ) {
        return resolve(player, anchor, null);
    }

    private static ResolvedSources resolve(
            ServerPlayer player,
            WorkbenchAnchor anchor,
            @javax.annotation.Nullable
            GunSmithCraftingSessionManager.GunSmithCraftingSession session
    ) {
        ArrayList<CraftingItemSource> sources = new ArrayList<>();
        ArrayList<ItemStack> externalStacks = new ArrayList<>();
        LinkedHashSet<CraftingSourceKey> sourceKeys =
                new LinkedHashSet<>();
        Set<Object> backendIdentities =
                Collections.newSetFromMap(new IdentityHashMap<>());

        PlayerInventorySource playerSource =
                new PlayerInventorySource(player);
        sources.add(playerSource);
        sourceKeys.add(playerSource.key());
        backendIdentities.add(playerSource.backendIdentity());

        if (CommonConfig.enableContainerReader()) {
            List<CraftingItemSource> nearby =
                    NearbyInventorySourceResolver.resolve(
                            player,
                            anchor.pos(),
                            CommonConfig.getContainerScanRadius(),
                            1
            );

            for (CraftingItemSource source : nearby) {
                try {
                    List<ItemStack> sourceDisplayStacks =
                            NearbyInventorySourceResolver
                                    .readAllDisplayStacks(source);
                    addUniqueSource(
                            sources,
                            externalStacks,
                            sourceKeys,
                            backendIdentities,
                            source,
                            sourceDisplayStacks,
                            MAX_EXTERNAL_STACKS
                    );
                } catch (RuntimeException exception) {
                    LOGGER.warn(
                            "Skipping unreadable gunsmith source {}",
                            source.key(),
                            exception
                    );
                }
            }
        }

        List<CraftingSourceKey> immutableKeys =
                List.copyOf(sourceKeys);
        if (session != null) {
            session.updateSourceKeys(immutableKeys);
        }

        return new ResolvedSources(
                Collections.unmodifiableList(
                        new ArrayList<>(sources)
                ),
                Collections.unmodifiableList(
                        copyStacks(externalStacks)
                ),
                Collections.unmodifiableList(
                        new ArrayList<>(immutableKeys)
                )
        );
    }

    static boolean addUniqueSource(
            List<CraftingItemSource> sources,
            List<ItemStack> externalStacks,
            Set<CraftingSourceKey> sourceKeys,
            Set<Object> backendIdentities,
            CraftingItemSource source,
            List<ItemStack> displayStacks,
            int maxExternalStacks
    ) {
        if (sourceKeys.contains(source.key())
                || backendIdentities.contains(
                source.backendIdentity()
        )) {
            return false;
        }

        if (externalStacks.size() + displayStacks.size()
                > maxExternalStacks) {
            return false;
        }

        sourceKeys.add(source.key());
        backendIdentities.add(source.backendIdentity());
        sources.add(source);
        externalStacks.addAll(displayStacks);
        return true;
    }

    private static ArrayList<ItemStack> copyStacks(
            List<ItemStack> stacks
    ) {
        ArrayList<ItemStack> copies =
                new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(stack.copy());
        }
        return copies;
    }

    public record ResolvedSources(
            List<CraftingItemSource> sources,
            List<ItemStack> externalStacks,
            List<CraftingSourceKey> sourceKeys
    ) {
    }
}
