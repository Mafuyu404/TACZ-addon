package com.mafuyu404.taczaddon.init.crafting;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared server-side scanner for nearby loaded block inventories.
 *
 * This class owns only the generic physical-source discovery contract. It
 * never enforces a Gunsmith presentation limit and never loads chunks. Callers
 * are responsible for their own display policies and authoritative anchors.
 */
public final class NearbyInventorySourceResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private NearbyInventorySourceResolver() {
    }

    public static List<CraftingItemSource> resolve(
            ServerPlayer player,
            BlockPos anchor,
            int horizontalRadius,
            int verticalRadius
    ) {
        Level level = player.level();
        BlockPos origin = anchor.immutable();
        int horizontal = Math.max(0, horizontalRadius);
        int vertical = Math.max(0, verticalRadius);

        BlockPos min = origin.offset(
                -horizontal,
                -vertical,
                -horizontal
        );
        BlockPos max = origin.offset(
                horizontal,
                vertical,
                horizontal
        );

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
            positions.add(mutable.immutable());
        }
        positions.sort(Comparator.comparingLong(BlockPos::asLong));

        ArrayList<CraftingItemSource> sources = new ArrayList<>();
        LinkedHashSet<CraftingSourceKey> sourceKeys =
                new LinkedHashSet<>();
        Set<Object> backendIdentities =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (BlockPos pos : positions) {
            if (pos.equals(origin) || !level.isLoaded(pos)) {
                continue;
            }

            try {
                ContainerItemSource source =
                        new ContainerItemSource(level, pos);
                if (!source.hasUsableBackend()
                        || !sourceKeys.add(source.key())
                        || !backendIdentities.add(
                        source.backendIdentity()
                )) {
                    continue;
                }

                sources.add(source);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Skipping unreadable nearby container source at {}",
                        pos,
                        exception
                );
            }
        }

        return List.copyOf(sources);
    }

    /**
     * Read-only helper retained for resolver callers that need display-only
     * copies. It is deliberately independent of Gunsmith stack limits.
     */
    public static List<ItemStack> readAllDisplayStacks(
            CraftingItemSource source
    ) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        int slots = source.slotCount();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack != null && !stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return List.copyOf(stacks);
    }
}
