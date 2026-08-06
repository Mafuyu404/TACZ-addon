package com.mafuyu404.taczaddon.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiberateAttachmentServiceTest {
    @Test
    void rejectsNegativeAndPastEndSlots() {
        assertFalse(
                LiberateAttachmentService.isSlotInRange(41, -1)
        );
        assertFalse(
                LiberateAttachmentService.isSlotInRange(41, 41)
        );
        assertFalse(
                LiberateAttachmentService.isSlotInRange(0, 0)
        );
    }

    @Test
    void acceptsOnlySlotsInsideInventory() {
        assertTrue(
                LiberateAttachmentService.isSlotInRange(41, 0)
        );
        assertTrue(
                LiberateAttachmentService.isSlotInRange(41, 40)
        );
    }
}
