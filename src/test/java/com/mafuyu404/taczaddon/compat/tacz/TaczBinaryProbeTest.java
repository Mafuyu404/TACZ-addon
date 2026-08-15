package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.compat.tacz.contract.FeatureContract;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczBinaryProbeTest {
    private static final List<TaczFeature> FRAGILE_FEATURES = List.of(
            TaczFeature.BACKPACK_AMMO_QUERY,
            TaczFeature.BACKPACK_AMMO_CONSUME,
            TaczFeature.LIBERATED_REFIT,
            TaczFeature.GUNSMITH_SESSION,
            TaczFeature.GUNSMITH_SCREEN_ACCESS,
            TaczFeature.GUNSMITH_EXTERNAL_SOURCE_VIEW,
            TaczFeature.GUNSMITH_CRAFT_BRIDGE,
            TaczFeature.GUNSMITH_BROWSE_MEMORY,
            TaczFeature.FAST_SWAP,
            TaczFeature.SHOOT_WHILE_RELOADING,
            TaczFeature.SLIDE_SHOOT,
            TaczFeature.HUD_AMMO,
            TaczFeature.TOOLTIP_EXTENSION,
            TaczFeature.CLIENT_ANIMATION,
            TaczFeature.BETTER_MELEE,
            TaczFeature.AIM_CAMERA,
            TaczFeature.CRAWL_DISABLE,
            TaczFeature.TACZ_SSR5_CROSSHAIR
    );

    @Test
    void everyVersionAdapterContractPassesAgainstResolvedTaCZ()
            throws Exception {
        for (TaczFeature feature : FRAGILE_FEATURES) {
            FeatureContract contract =
                    TaczContractRegistry.contractFor(feature);
            assertTrue(
                    contract != null
                            && !contract.classes().isEmpty(),
                    feature + " must have a binary contract"
            );
            TaczBinaryProbe.ProbeResult result =
                    TaczBinaryProbe.inspect(contract);
            assertTrue(
                    result.passed(),
                    feature + " contract failed: " + result.detail()
            );
        }
    }
}
