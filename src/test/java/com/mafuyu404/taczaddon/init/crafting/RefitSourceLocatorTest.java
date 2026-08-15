package com.mafuyu404.taczaddon.init.crafting;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefitSourceLocatorTest {
    @Test
    void roundTripsDimensionPositionAndSlot() {
        RefitSourceLocator locator = new RefitSourceLocator(
                Level.OVERWORLD,
                new BlockPos(10, 70, -5),
                3
        );

        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );
        locator.encode(buffer);
        RefitSourceLocator decoded =
                RefitSourceLocator.decode(buffer);

        assertEquals(locator, decoded);
        assertEquals(0, buffer.readableBytes());
    }
}
