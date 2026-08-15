package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.compat.tacz.contract.ClassContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.FeatureContract;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczCompatibilityGateTest {
    private static final Path PROJECT_ROOT =
            Path.of("").toAbsolutePath().normalize();

    @Test
    void versionProfileIsExact() {
        assertEquals(
                TaczCompatibilityProfile.TACZ_1_1_8_HOTFIX,
                TaczVersionReader.profileForVersion("1.1.8-hotfix")
        );
        assertEquals(
                TaczCompatibilityProfile.UNKNOWN,
                TaczVersionReader.profileForVersion("1.1.8")
        );
        assertEquals(
                TaczCompatibilityProfile.UNKNOWN,
                TaczVersionReader.profileForVersion("1.1.8.1")
        );
        assertEquals(
                TaczCompatibilityProfile.UNKNOWN,
                TaczVersionReader.profileForVersion("1.1.9")
        );
        assertEquals(
                TaczCompatibilityProfile.UNKNOWN,
                TaczVersionReader.profileForVersion("unknown")
        );
    }

    @Test
    void featureMismatchDoesNotMutateProfileOrOtherFeatures() {
        assertEquals(
                TaczCompatibilityProfile.TACZ_1_1_8_HOTFIX,
                TaczCompatibility.profile()
        );

        TaczMixinBinding broken = new TaczMixinBinding(
                "com.example.BrokenMixin",
                TaczFeature.BACKPACK_AMMO_QUERY,
                new FeatureContract(
                        TaczFeature.BACKPACK_AMMO_QUERY,
                        TaczContractRegistry.PROFILE_ID,
                        new ClassContract("com.example.Missing")
                ),
                CompatibilityScope.VERSION_BOUND
        );

        assertFalse(TaczCompatibility.isMixinBindingAvailable(broken));
        assertEquals(
                TaczCompatibilityProfile.TACZ_1_1_8_HOTFIX,
                TaczCompatibility.profile()
        );
        assertEquals(
                TaczFeatureStatus.SUPPORTED,
                TaczCompatibility.status(TaczFeature.CLIENT_ANIMATION)
        );
    }

    @Test
    void unregisteredMixinFailsClosed() {
        TaczAddonMixinPlugin plugin = new TaczAddonMixinPlugin();
        assertFalse(plugin.shouldApplyMixin(
                "com.example.Target",
                "com.example.UnregisteredMixin"
        ));
    }

    @Test
    void everyTaCZMixinConfigEntryHasBinding() throws IOException {
        String json = Files.readString(
                PROJECT_ROOT.resolve(
                        "src/main/resources/taczaddon.tacz.mixins.json"
                ),
                StandardCharsets.UTF_8
        );
        for (String entry : entries(json)) {
            String mixinClass = "com.mafuyu404.taczaddon.mixin.tacz."
                    + entry;
            assertNotNull(
                    TaczContractRegistry.bindingForMixin(mixinClass),
                    mixinClass + " has no registry binding"
            );
        }
    }

    @Test
    void slideContractUsesPublicThreeArgumentOverload() {
        TaczMixinBinding binding =
                TaczContractRegistry.bindingForMixin(
                        "com.mafuyu404.taczaddon.mixin.tacz.v1_1_8."
                                + "LivingEntityShootMixin"
                );
        assertNotNull(binding);
        String contractText = binding.contract().toString();
        assertTrue(contractText.contains(
                "Ljava/util/function/Supplier;"
                        + "Ljava/util/function/Supplier;J)"
        ));
        assertFalse(contractText.contains("JFZ)"));
    }

    private static List<String> entries(String json) {
        List<String> result = new ArrayList<>();
        for (String line : json.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                continue;
            }
            if (trimmed.startsWith("\"")
                    && trimmed.endsWith("\",")
                    || trimmed.startsWith("\"")
                    && trimmed.endsWith("\"")) {
                result.add(trimmed.substring(
                        1,
                        trimmed.length() - (trimmed.endsWith(",") ? 2 : 1)
                ));
            }
        }
        return result;
    }
}
