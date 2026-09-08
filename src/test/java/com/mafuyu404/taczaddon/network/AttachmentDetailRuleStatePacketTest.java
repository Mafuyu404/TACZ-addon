package com.mafuyu404.taczaddon.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentDetailRuleStatePacketTest {
    @Test
    void encodesOnlyOneBoolean() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        AttachmentDetailRuleStatePacket.encode(
                new AttachmentDetailRuleStatePacket(true),
                buffer
        );

        assertEquals(1, buffer.readableBytes());
        assertTrue(
                AttachmentDetailRuleStatePacket.decode(buffer).enabled()
        );
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void preservesFalse() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        AttachmentDetailRuleStatePacket.encode(
                new AttachmentDetailRuleStatePacket(false),
                buffer
        );

        assertFalse(
                AttachmentDetailRuleStatePacket.decode(buffer).enabled()
        );
    }
}
