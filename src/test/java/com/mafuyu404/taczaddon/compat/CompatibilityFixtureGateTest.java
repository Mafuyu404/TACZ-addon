package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.testutil.CompatibilityFixtureGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Gradle verification pipeline must fail - not silently pass - when the
 * real upstream ABI fixtures are unavailable. The explicit development property
 * is the only way to downgrade that to a skip.
 */
class CompatibilityFixtureGateTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(
                CompatibilityFixtureGate.ALLOW_MISSING_PROPERTY
        );
    }

    @Test
    void presentFixtureIsAlwaysAccepted() {
        System.setProperty(
                CompatibilityFixtureGate.ALLOW_MISSING_PROPERTY,
                "false"
        );
        assertDoesNotThrow(() -> CompatibilityFixtureGate.require(
                true,
                "unused"
        ));
    }

    @Test
    void missingFixtureFailsByDefault() {
        System.setProperty(
                CompatibilityFixtureGate.ALLOW_MISSING_PROPERTY,
                "false"
        );
        AssertionFailedError failure = assertThrows(
                AssertionFailedError.class,
                () -> CompatibilityFixtureGate.require(
                        false,
                        "fixture missing"
                )
        );
        assertTrue(failure.getMessage().contains("fixture missing"));
        assertTrue(
                failure.getMessage().contains(
                        CompatibilityFixtureGate.ALLOW_MISSING_PROPERTY
                ),
                "the error must explain how to opt into development mode"
        );
    }

    @Test
    void missingFixtureSkipsOnlyInExplicitDevelopmentMode() {
        System.setProperty(
                CompatibilityFixtureGate.ALLOW_MISSING_PROPERTY,
                "true"
        );
        assertThrows(
                TestAbortedException.class,
                () -> CompatibilityFixtureGate.require(
                        false,
                        "fixture missing"
                )
        );
    }
}
