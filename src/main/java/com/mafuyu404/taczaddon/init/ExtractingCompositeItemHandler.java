package com.mafuyu404.taczaddon.init;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ExtractingCompositeItemHandler extends ReadOnlyCompositeItemHandler {
    public ExtractingCompositeItemHandler(Collection<CompositeItemSource> sources) {
        super(sources);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        CompositeItemSource source = getSource(slot);
        if (source == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return source.handler().extractItem(source.slot(), amount, simulate);
    }
}
