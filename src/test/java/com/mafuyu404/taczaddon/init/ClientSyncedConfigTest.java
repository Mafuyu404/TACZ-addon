package com.mafuyu404.taczaddon.init;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSyncedConfigTest {
    @AfterEach
    void reset() {
        ClientSyncedConfig.resetToSafeDefaults();
    }

    @Test
    void resetFailsClosedAcrossConnections() {
        ClientSyncedConfig.setLiberateAttachment(true);
        assertTrue(ClientSyncedConfig.liberateAttachment());

        ClientSyncedConfig.resetToSafeDefaults();

        assertFalse(ClientSyncedConfig.liberateAttachment());
    }
}
