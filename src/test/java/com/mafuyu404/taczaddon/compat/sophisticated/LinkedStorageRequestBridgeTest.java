package com.mafuyu404.taczaddon.compat.sophisticated;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.RequestLinkedStorageContentsPayload;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkedStorageRequestBridgeTest {
    @Test void selectedCoreRequestPreservesGroupAndRevision() {
        UUID group = UUID.randomUUID();
        assertEquals(new RequestLinkedStorageContentsPayload(group, -1L),
                LinkedStorageRequestBridge.create(group, -1L));
    }

    @Test void fallsBackOnlyWhenTheCorePayloadIsAbsent() throws Exception {
        UUID group = UUID.randomUUID();
        var constructor = LinkedStorageRequestBridge.resolve(getClass().getClassLoader(),
                "missing.CoreRequest", LegacyRequest.class.getName());
        assertEquals(new LegacyRequest(group, 7L), constructor.newInstance(group, 7L));
        // A present but incompatible API must fail its probe, not select an obsolete protocol.
        assertThrows(SophisticatedCompatibilityException.class, () -> LinkedStorageRequestBridge.resolve(
                getClass().getClassLoader(), String.class.getName(), LegacyRequest.class.getName()));
    }

    public record LegacyRequest(UUID group, long revision) implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() {
            return new Type<>(ResourceLocation.fromNamespaceAndPath("test", "legacy_request"));
        }
    }
}
