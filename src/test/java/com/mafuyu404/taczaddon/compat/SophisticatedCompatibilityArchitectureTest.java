package com.mafuyu404.taczaddon.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SophisticatedCompatibilityArchitectureTest {
    private static final Path PROJECT_ROOT =
            Path.of("").toAbsolutePath().normalize();

    @Test
    void obsoleteBackpackIdentityApisAreRemoved()
            throws IOException {
        String outer = read(
                "src/main/java/com/mafuyu404/taczaddon/compat/"
                        + "SophisticatedBackpacksCompat.java"
        );
        String inner = read(
                "src/main/java/com/mafuyu404/taczaddon/compat/"
                        + "SophisticatedBackpacksCompatInner.java"
        );

        for (String obsolete : new String[] {
                "getAllInventoryBackpack",
                "modifyInventoryBackpack",
                "getItemsFromInventoryBackpack",
                "getItemsFromBackpackItem",
                "getItemsFromBackpackBLock",
                "modifyBlockBackpack",
                "getItemsFromBackpackContext"
        }) {
            assertFalse(
                    outer.contains(obsolete),
                    "outer facade must not expose " + obsolete
            );
            assertFalse(
                    inner.contains(obsolete),
                    "inner must not retain " + obsolete
            );
        }

        assertFalse(inner.contains("ItemStack.matches(backpack"));
        assertTrue(inner.contains("new BackpackContext.Item("));
        assertTrue(inner.contains("handlerName"));
        assertTrue(inner.contains("identifier"));
        assertTrue(inner.contains("slot"));
    }

    @Test
    void developmentDependenciesRemainPinned()
            throws IOException {
        String gradle = read("build.gradle");
        assertTrue(gradle.contains(
                "compileOnly fg.deobf(\"curse.maven:"
                        + "sophisticated-core-618298:8815743\")"
        ));
        assertTrue(gradle.contains(
                "compileOnly fg.deobf(\"curse.maven:"
                        + "sophisticated-backpacks-422301:8817428\")"
        ));
        assertTrue(gradle.contains(
                "runtimeOnly fg.deobf(\"curse.maven:"
                        + "sophisticated-storage-619320:8719374\")"
        ));
        assertFalse(gradle.contains(
                "implementation fg.deobf(\"curse.maven:"
                        + "sophisticated-storage-619320"
        ));

    }

    @Test
    void hudCacheNoLongerRebuildsEveryTick()
            throws IOException {
        String clientEvent = read(
                "src/main/java/com/mafuyu404/taczaddon/event/"
                        + "ClientEvent.java"
        );
        assertTrue(clientEvent.contains(
                "BACKPACK_HUD_REFRESH_INTERVAL_TICKS = 5"
        ));
        assertFalse(clientEvent.contains(
                "getItemsFromInventoryBackpack"
        ));
    }

    private static String read(String relativePath)
            throws IOException {
        return Files.readString(
                PROJECT_ROOT.resolve(relativePath),
                StandardCharsets.UTF_8
        );
    }
}
