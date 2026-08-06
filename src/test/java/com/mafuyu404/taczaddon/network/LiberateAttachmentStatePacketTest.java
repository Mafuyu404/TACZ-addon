package com.mafuyu404.taczaddon.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiberateAttachmentStatePacketTest {
    @Test
    void encodesOnlyOneBoolean() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        LiberateAttachmentStatePacket.encode(
                new LiberateAttachmentStatePacket(true),
                buffer
        );

        assertEquals(1, buffer.readableBytes());
        assertTrue(
                LiberateAttachmentStatePacket.decode(buffer).enabled()
        );
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void preservesFalse() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        LiberateAttachmentStatePacket.encode(
                new LiberateAttachmentStatePacket(false),
                buffer
        );

        assertFalse(
                LiberateAttachmentStatePacket.decode(buffer).enabled()
        );
    }
}
