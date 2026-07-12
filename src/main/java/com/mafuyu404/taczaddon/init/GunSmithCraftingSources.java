package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.init.crafting.ContainerItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingItemSource;
import com.mafuyu404.taczaddon.init.crafting.CraftingSourceKey;
import com.mafuyu404.taczaddon.init.crafting.PlayerInventorySource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public final class GunSmithCraftingSources {
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

        PlayerInventorySource playerSource =
                new PlayerInventorySource(player);
        sources.add(playerSource);
        sourceKeys.add(playerSource.key());

        if (CommonConfig.enableContainerReader()) {
            resolveNearbyContainers(
                    player,
                    session,
                    sources,
                    externalStacks,
                    sourceKeys
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
            Set<CraftingSourceKey> sourceKeys
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

            ContainerItemSource source =
                    new ContainerItemSource(level, pos);
            CraftingSourceKey key = source.key();

            if (!source.hasUsableBackend()
                    || sourceKeys.contains(key)) {
                continue;
            }

            List<ItemStack> sourceDisplayStacks =
                    readAllDisplayStacks(source);

            /*
             * Keep the server's usable-source set identical to what the client
             * can represent. Never partially expose a source and then allow
             * crafting from its hidden slots.
             */
            if (externalStacks.size() + sourceDisplayStacks.size()
                    > MAX_EXTERNAL_STACKS) {
                continue;
            }

            sourceKeys.add(key);
            sources.add(source);
            externalStacks.addAll(sourceDisplayStacks);
        }
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
