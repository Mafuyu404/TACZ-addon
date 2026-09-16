package com.mafuyu404.taczaddon.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wire contract of the gunsmith source snapshot.
 *
 * <p>The aggregate count array carries recipe-input information and must not
 * be silently cleared just because a recipe carries more than ordinary
 * 32-input recipes.
 */
class GunSmithSourceSnapshotPacketTest {

    @Test
    void thirtyThreeAggregateInputsSurviveTheRoundTrip() {
        int[] counts = new int[33];

        for (int i = 0; i < counts.length; i++) {
            counts[i] = i + 1;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        GunSmithSourceSnapshotPacket.encode(
                new GunSmithSourceSnapshotPacket(
                        7,
                        11L,
                        3L,
                        true,
                        false,
                        List.of(),
                        counts
                ),
                buffer
        );

        GunSmithSourceSnapshotPacket decoded =
                GunSmithSourceSnapshotPacket.decode(buffer);

        assertEquals(33, decoded.aggregateCounts().length);
        assertArrayEquals(counts, decoded.aggregateCounts());
        assertTrue(decoded.externalSourcesAuthorized());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void aggregateArrayBeyondTheWireBoundIsRejectedAtDecode() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );
        buffer.writeInt(1);
        buffer.writeLong(2L);
        buffer.writeLong(3L);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeInt(0);
        buffer.writeInt(
                GunSmithSourceSnapshotPacket.MAX_AGGREGATE_INPUTS + 1
        );

        assertThrows(
                DecoderException.class,
                () -> GunSmithSourceSnapshotPacket.decode(buffer)
        );
    }
}
