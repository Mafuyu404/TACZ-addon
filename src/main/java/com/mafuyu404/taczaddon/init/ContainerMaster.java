package com.mafuyu404.taczaddon.init;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class ContainerMaster {
    public static ArrayList<ItemStack> readContainerFromPos(Level level, BlockPos pos) {
        ArrayList<ItemStack> items = new ArrayList<>();

        if (!level.isLoaded(pos)) {
            return items;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }

        return items;
    }
}
