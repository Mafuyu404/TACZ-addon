package com.mafuyu404.taczaddon.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmmoConsumptionOrchestratorTest {
    @Test
    void nativeAlreadySatisfiesRequestCallsNoSources() {
        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                30,
                remaining -> {
                    throw new AssertionError(
                            "Beyond must not run when already satisfied"
                    );
                },
                remaining -> {
                    throw new AssertionError(
                            "Backpack must not run when already satisfied"
                    );
                }
        );

        assertEquals(30, consumed);
    }

    @Test
    void beyondFullySatisfiesDeficitBeforeBackpack() {
        List<Integer> calls = new ArrayList<>();

        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                10,
                remaining -> {
                    calls.add(remaining);
                    return 20;
                },
                remaining -> {
                    throw new AssertionError(
                            "Backpack must not run after Beyond fills request"
                    );
                }
        );

        assertEquals(30, consumed);
        assertEquals(List.of(20), calls);
    }

    @Test
    void beyondPartiallySatisfiesThenBackpackFills() {
        List<Integer> calls = new ArrayList<>();

        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                10,
                remaining -> {
                    calls.add(remaining);
                    return 12;
                },
                remaining -> {
                    calls.add(remaining);
                    return 8;
                }
        );

        assertEquals(30, consumed);
        assertEquals(List.of(20, 8), calls);
    }

    @Test
    void absentBeyondGoesDirectlyToBackpackFallback() {
        List<Integer> calls = new ArrayList<>();

        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                10,
                remaining -> 0,
                remaining -> {
                    calls.add(remaining);
                    return 20;
                }
        );

        assertEquals(30, consumed);
        assertEquals(List.of(20), calls);
    }

    @Test
    void bothSourcesPartiallySatisfy() {
        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                5,
                remaining -> 7,
                remaining -> 6
        );

        assertEquals(18, consumed);
    }

    @Test
    void overReportingBeyondIsClampedAndBackpackSkips() {
        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                10,
                remaining -> 100,
                remaining -> {
                    throw new AssertionError(
                            "Backpack must not run after Beyond fills request"
                    );
                }
        );

        assertEquals(30, consumed);
    }

    @Test
    void negativeBeyondReturnContributesZero() {
        int consumed = AmmoConsumptionOrchestrator.consumeRemaining(
                30,
                10,
                remaining -> -5,
                remaining -> 0
        );

        assertEquals(10, consumed);
    }

    @Test
    void invalidRequestReturnsZeroWithoutSourceCalls() {
        assertEquals(
                0,
                AmmoConsumptionOrchestrator.consumeRemaining(
                        0,
                        5,
                        remaining -> {
                            throw new AssertionError(
                                    "Beyond must not run for invalid request"
                            );
                        },
                        remaining -> {
                            throw new AssertionError(
                                    "Backpack must not run for invalid request"
                            );
                        }
                )
        );
        assertEquals(
                0,
                AmmoConsumptionOrchestrator.consumeRemaining(
                        -1,
                        5,
                        remaining -> {
                            throw new AssertionError(
                                    "Beyond must not run for invalid request"
                            );
                        },
                        remaining -> {
                            throw new AssertionError(
                                    "Backpack must not run for invalid request"
                            );
                        }
                )
        );
    }

    @Test
    void clampConsumedHandlesAllBounds() {
        assertEquals(0, AmmoConsumptionOrchestrator.clampConsumed(10, -5));
        assertEquals(0, AmmoConsumptionOrchestrator.clampConsumed(10, 0));
        assertEquals(10, AmmoConsumptionOrchestrator.clampConsumed(10, 10));
        assertEquals(10, AmmoConsumptionOrchestrator.clampConsumed(10, 99));
        assertEquals(0, AmmoConsumptionOrchestrator.clampConsumed(0, 10));
    }
}
