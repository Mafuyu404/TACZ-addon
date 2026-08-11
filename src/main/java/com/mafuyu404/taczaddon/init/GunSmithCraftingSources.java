package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.init.crafting.ContainerItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.mafuyu404.taczaddon.init.crafting.PlayerInventorySource;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
            resolveNearbyContainers(
                    player,
                    session,
                    sources,
                    externalStacks,
                    sourceKeys,
                    backendIdentities
            );
        }

        List<CraftingSourceKey> immutableKeys =
                List.copyOf(sourceKeys);
        session.updateSourceKeys(immutableKeys);

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

    private static void resolveNearbyContainers(
            ServerPlayer player,
            GunSmithCraftingSessionManager.GunSmithCraftingSession session,
            List<CraftingItemSource> sources,
            List<ItemStack> externalStacks,
            Set<CraftingSourceKey> sourceKeys,
            Set<Object> backendIdentities
    ) {
        Level level = player.level();
        BlockPos tablePos = session.getTablePos();
        int radius = CommonConfig.getContainerScanRadius();

        BlockPos min = tablePos.offset(-radius, -1, -radius);
        BlockPos max = tablePos.offset(radius, 1, radius);

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
            positions.add(mutable.immutable());
        }
        positions.sort(Comparator.comparingLong(BlockPos::asLong));

        for (BlockPos pos : positions) {
            if (pos.equals(tablePos) || !level.isLoaded(pos)) {
                continue;
            }

            try {
                ContainerItemSource source =
                        new ContainerItemSource(level, pos);

                if (!source.hasUsableBackend()) {
                    continue;
                }

                List<ItemStack> sourceDisplayStacks =
                        readAllDisplayStacks(source);

                /*
                 * Keep the server's usable-source set identical to what the
                 * client can represent. Never partially expose a source and
                 * then allow crafting from its hidden slots.
                 */
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
                        "Skipping unreadable gunsmith container source at {}",
                        pos,
                        exception
                );
            }
        }
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

    private static List<ItemStack> readAllDisplayStacks(
            CraftingItemSource source
    ) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        int slots = source.slotCount();

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        return stacks;
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
