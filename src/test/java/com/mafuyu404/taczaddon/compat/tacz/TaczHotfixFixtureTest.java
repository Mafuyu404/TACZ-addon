package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.testutil.CompatibilityFixtures;
import org.junit.jupiter.api.Test;

import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class TaczHotfixFixtureTest {
    @Test
    void bothPublishedReleasesSatisfyEveryAdapterContract() throws Exception {
        for (String generation : new String[]{"hotfix", "hotfix2"}) {
            var path = CompatibilityFixtures.jar("tacz", generation, "tacz");
            CompatibilityFixtures.requireVersion(path, "1.1.8-" + generation, "TaCZ " + generation);
            try (var jar = new JarFile(path.toFile())) {
                int checked = 0;
                for (var feature : TaczFeature.values()) {
                    var contract = TaczContractRegistry.contractFor(feature);
                    if (contract == null || contract.classes().isEmpty()) continue;
                    var result = TaczBinaryProbe.inspect(contract, name -> {
                        String resource = name.replace('.', '/') + ".class";
                        var entry = jar.getJarEntry(resource);
                        try (var input = entry != null ? jar.getInputStream(entry)
                                : resource.startsWith("com/tacz/") ? null
                                : getClass().getClassLoader().getResourceAsStream(resource)) {
                            return input == null ? null : input.readAllBytes();
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
                    assertTrue(result.passed(), generation + " / " + feature + ": " + result.detail());
                    checked++;
                }
                assertTrue(checked > 20, "Must cover the version-bound adapter contracts");
            }
        }
    }
}
