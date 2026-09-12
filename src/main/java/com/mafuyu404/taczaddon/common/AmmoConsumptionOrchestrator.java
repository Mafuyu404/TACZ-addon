package com.mafuyu404.taczaddon.common;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.IntUnaryOperator;

/**
 * Internal arithmetic/orchestration helper for supplemental ammo consumption.
 *
 * This deliberately does not know about TaCZ, Beyond Integration, or
 * Sophisticated Backpacks. The Mixin supplies the two source callbacks, so the
 * ordering here is always: consumed-so-far, Beyond-compatible source, then
 * backpack fallback.
 */
public final class AmmoConsumptionOrchestrator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AmmoConsumptionOrchestrator() {
    }

    public static int consumeRemaining(
            int requested,
            int consumedSoFar,
            IntUnaryOperator beyondCompat,
            IntUnaryOperator backpackFallback
    ) {
        return consumeRemaining(
                requested,
                consumedSoFar,
                false,
                beyondCompat,
                backpackFallback
        );
    }

    public static int consumeRemaining(
            int requested,
            int consumedSoFar,
            boolean beyondActive,
            IntUnaryOperator beyondCompat,
            IntUnaryOperator backpackFallback
    ) {
        int current = clampConsumed(
                requested,
                consumedSoFar
        );
        int remaining = Math.max(
                0,
                requested - current
        );
        if (remaining <= 0) {
            return current;
        }

        int consumedBeforeExternal = current;
        int beyondConsumed;
        try {
            beyondConsumed = clampConsumed(
                    remaining,
                    beyondCompat.applyAsInt(remaining)
            );
        } catch (IncompleteConsumptionException incomplete) {
            // Only consumption confirmed before the opaque external call is known.
            // Stop this request; a disabled source contributes zero on future requests.
            return current;
        }
        current = clampConsumed(
                requested,
                current + beyondConsumed
        );
        remaining = Math.max(
                0,
                requested - current
        );

        int remainingBeforeBackpack = remaining;
        int backpackConsumed = 0;
        if (remaining > 0) {
            backpackConsumed = clampConsumed(
                    remaining,
                    backpackFallback.applyAsInt(remaining)
            );
            current = clampConsumed(
                    requested,
                    current + backpackConsumed
            );
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "[TACZ-addon/AmmoFallback] "
                            + "requested={}, consumedBeforeExternal={}, "
                            + "beyondActive={}, beyondConsumed={}, "
                            + "remainingBeforeBackpack={}, "
                            + "backpackConsumed={}, final={}",
                    requested,
                    consumedBeforeExternal,
                    beyondActive,
                    beyondConsumed,
                    remainingBeforeBackpack,
                    backpackConsumed,
                    current
            );
        }
        return current;
    }

    /** Signals an external mutation whose committed amount could not be reported. */
    public static final class IncompleteConsumptionException extends RuntimeException {
        public IncompleteConsumptionException(LinkageError cause) {
            super("External ammo consumption did not complete", cause);
        }
    }

    public static int clampConsumed(
            int requested,
            int consumed
    ) {
        if (requested <= 0) {
            return 0;
        }
        return Math.max(
                0,
                Math.min(requested, consumed)
        );
    }
}
