package com.mafuyu404.taczaddon.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Invoker("renderSlotHighlight")
    static void taczaddon$renderSlotHighlight(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int blitOffset,
            int color
    ) {
        throw new AssertionError();
    }
}