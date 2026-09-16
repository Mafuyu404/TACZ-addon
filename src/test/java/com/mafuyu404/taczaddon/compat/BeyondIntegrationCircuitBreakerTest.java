package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.common.AmmoConsumptionOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BeyondIntegrationCircuitBreakerTest {
    @AfterEach
    void reset() throws Exception {
        var latch = BeyondIntegrationCompat.class.getDeclaredField("linkageBroken");
        latch.setAccessible(true);
        latch.setBoolean(null, false);
        var warning = BeyondIntegrationCompat.class.getDeclaredField("LINKAGE_WARNING_LOGGED");
        warning.setAccessible(true);
        ((AtomicBoolean) warning.get(null)).set(false);
    }

    @Test
    void opaquePartialFailureStopsThisRequestThenOtherSourcesResume() {
        AtomicInteger externalMutations = new AtomicInteger();
        var outcome = AmmoConsumptionOrchestrator.consumeRemaining(10, 2,
                remaining -> AmmoConsumptionOrchestrator.ConsumptionOutcome.confirmed(
                        BeyondIntegrationCompat.runGuarded(remaining, () -> {
                            externalMutations.incrementAndGet();
                            throw new NoSuchMethodError("failed after partial external mutation");
                        })),
                remaining -> { fail("Unknown external consumption must not trigger more extraction"); return null; });
        assertEquals(2, outcome.consumed());
        assertEquals(AmmoConsumptionOrchestrator.Status.STOPPED_UNKNOWN, outcome.status());
        assertEquals(1, externalMutations.get());

        assertEquals(10, AmmoConsumptionOrchestrator.consumeRemaining(10, 2,
                remaining -> AmmoConsumptionOrchestrator.ConsumptionOutcome.confirmed(
                        BeyondIntegrationCompat.runGuarded(remaining, () -> {
                            fail("Broken external bridge retried"); return 0;
                        })),
                remaining -> AmmoConsumptionOrchestrator.ConsumptionOutcome
                        .confirmed(remaining)).consumed());
        assertTrue(CuriosCompat.runGuarded(() -> true));
        assertTrue(JeiCompat.runGuarded(() -> true));
    }

    @Test
    void successfulBridgeIsBoundedAndMissesDoNotTripBreaker() {
        assertEquals(0, BeyondIntegrationCompat.runGuarded(4, () -> -1));
        assertEquals(4, BeyondIntegrationCompat.runGuarded(4, () -> 10));
        assertEquals(2, BeyondIntegrationCompat.runGuarded(4, () -> 2));
    }

    @Test
    void unrelatedFailuresAreNotSwallowed() {
        assertThrows(IllegalStateException.class, () -> BeyondIntegrationCompat.runGuarded(4,
                () -> { throw new IllegalStateException("fixture"); }));
        assertThrows(AssertionError.class, () -> BeyondIntegrationCompat.runGuarded(4,
                () -> { throw new AssertionError("fixture"); }));
        assertEquals(4, BeyondIntegrationCompat.runGuarded(4, () -> 4));
    }
}
