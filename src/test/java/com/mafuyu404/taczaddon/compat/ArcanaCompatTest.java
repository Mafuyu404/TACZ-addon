package com.mafuyu404.taczaddon.compat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcanaCompatTest {
    @Test
    void absentModKeepsNativeMessageCompatibilityDisabled() {
        AtomicBoolean probed = new AtomicBoolean(false);

        assertFalse(ArcanaCompat.canHandleNativeAttachmentMessages(
                false,
                className -> {
                    probed.set(true);
                    return true;
                }
        ));
        assertFalse(probed.get());
    }

    @Test
    void missingCompatClassFailsClosed() {
        assertFalse(ArcanaCompat.canHandleNativeAttachmentMessages(
                true,
                className -> false
        ));
    }

    @Test
    void loadedModWithCompatClassEnablesNativeMessages() {
        assertTrue(ArcanaCompat.canHandleNativeAttachmentMessages(
                true,
                className -> true
        ));
    }
}
