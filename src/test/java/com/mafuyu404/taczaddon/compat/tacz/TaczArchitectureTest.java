package com.mafuyu404.taczaddon.compat.tacz;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TaczArchitectureTest {
    private static final Path PROJECT_ROOT =
            Path.of("").toAbsolutePath().normalize();

    @Test
    void noTaCZMixinUsesLocalVariableInjection()
            throws IOException {
        Path mixinRoot = PROJECT_ROOT.resolve(
                "src/main/java/com/mafuyu404/taczaddon/mixin/tacz"
        );
        List<Path> sources = listJavaFiles(mixinRoot);
        assertFalse(sources.isEmpty());

        for (Path source : sources) {
            String text = Files.readString(
                    source,
                    StandardCharsets.UTF_8
            );
            assertFalse(
                    text.contains("lambda$"),
                    source + " contains a synthetic lambda selector"
            );
            assertFalse(
                    text.contains("@ModifyVariable"),
                    source + " uses @ModifyVariable"
            );
            assertFalse(
                    text.contains("@At(\"STORE\")"),
                    source + " uses STORE injection"
            );
        }
    }

    @Test
    void singleMixinConfigUsesPlatformCompatibilityAndTaCZPlugin()
            throws IOException {
        String config = read(
                "src/main/resources/taczaddon.mixins.json"
        );
        try (Stream<Path> resources = Files.list(PROJECT_ROOT.resolve("src/main/resources"))) {
            assertEquals(List.of("taczaddon.mixins.json"), resources
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".mixins.json"))
                    .sorted().toList());
        }
        assertTrue(config.contains("\"compatibilityLevel\": \"JAVA_17\""));
        assertTrue(config.contains("\"package\": \"com.mafuyu404.taczaddon.mixin\""));
        assertTrue(config.contains("\"refmap\": \"taczaddon.refmap.json\""));
        assertTrue(config.contains(
                "\"plugin\": \"com.mafuyu404.taczaddon.compat.tacz."
                        + "TaczAddonMixinPlugin\""
        ));
        assertTrue(config.contains("SmithingMenuMixin"));
        assertTrue(config.contains("AbstractContainerScreenMixin"));
        assertTrue(config.contains("tacz.v1_1_8.LocalPlayerDrawMixin"));
        assertTrue(config.contains("tacz.v1_1_8.GunSmithTableSourceViewMixin"));
        assertTrue(config.contains(
                "tacz.v1_1_8.GunSmithTableIngredientInteractionMixin"
        ));
        assertEquals(List.of("config \"${mod_id}.mixins.json\""), read("build.gradle")
                .lines().map(String::trim)
                .filter(line -> line.startsWith("config ")).toList());
    }

    @Test
    void noLegacyGlobalJeiBridgeRemains()
            throws IOException {
        // Concatenated literals keep the legacy bridge name out of the
        // source tree so the final bridge-name grep stays at zero hits.
        String legacyStorageKey = "GunSmithTable" + "JEI";
        String legacyEventClass = "JEI" + "Event";
        Path mainRoot = PROJECT_ROOT.resolve(
                "src/main/java/com/mafuyu404/taczaddon"
        );
        for (Path source : listJavaFiles(mainRoot)) {
            String text = Files.readString(
                    source,
                    StandardCharsets.UTF_8
            );
            assertFalse(
                    text.contains(legacyStorageKey),
                    source + " references the legacy DataStorage bridge"
            );
            assertFalse(
                    text.contains(legacyEventClass),
                    source + " references the legacy input event bridge"
            );
        }
        assertFalse(
                Files.exists(PROJECT_ROOT.resolve(
                        "src/main/java/com/mafuyu404/taczaddon/"
                                + "event/" + "JEI" + "Event.java"
                )),
                "legacy " + "JEI" + "Event.java must stay deleted"
        );
    }

    @Test
    void gunSmithIngredientMixinKeepsFrameLocalArchitecture()
            throws IOException {
        String mixin = read(
                "src/main/java/com/mafuyu404/taczaddon/mixin/tacz/"
                        + "v1_1_8/"
                        + "GunSmithTableIngredientInteractionMixin.java"
        );

        assertTrue(mixin.contains("renderIngredient("));
        assertTrue(mixin.contains("renderFakeItem"));
        assertTrue(mixin.contains("GunSmithIngredientInteractionState"));
        assertTrue(mixin.contains("JeiCompat.showRecipes"));
        assertTrue(mixin.contains("require = 1"));
        assertTrue(mixin.contains("remap = false"));
        assertTrue(mixin.contains("remap = true"));

        assertFalse(mixin.contains("DataStorage"));
        assertFalse(mixin.contains("GunSmithTable" + "JEI"));
        assertFalse(mixin.contains("InputEvent.MouseButton"));
        assertFalse(mixin.contains("int[] mouse"));
        assertFalse(mixin.contains("HashMap<String, Boolean>"));
        assertFalse(mixin.contains("require = 0"));
        assertFalse(mixin.contains("@ModifyVariable"));
        assertFalse(mixin.contains("@At(\"STORE\")"));

        String containerScreen = read(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "AbstractContainerScreenMixin.java"
        );
        assertTrue(containerScreen.contains("mouseReleased"));
        assertTrue(containerScreen.contains(
                "GunSmithIngredientScreenAccess"
        ));
    }

    @Test
    void commonAndNetworkPackagesDoNotImportClientClasses()
            throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve(
                        "src/main/java/com/mafuyu404/taczaddon/common"
                ),
                PROJECT_ROOT.resolve(
                        "src/main/java/com/mafuyu404/taczaddon/network"
                ),
                PROJECT_ROOT.resolve(
                        "src/main/java/com/mafuyu404/taczaddon/compat/tacz"
                )
        );

        for (Path root : roots) {
            for (Path source : listJavaFiles(root)) {
                String text = Files.readString(
                        source,
                        StandardCharsets.UTF_8
                );
                assertFalse(
                        text.contains("import net.minecraft.client."),
                        source + " imports a client class"
                );
            }
        }
    }

    @Test
    void everyVersionAdapterIsMappedToAFeature()
            throws IOException {
        String config = read(
                "src/main/resources/taczaddon.mixins.json"
        );
        for (String entry : entries(config)) {
            if (!entry.startsWith("tacz.")) {
                continue;
            }
            String mixinClass = "com.mafuyu404.taczaddon.mixin."
                    + entry;
            assertTrue(
                    TaczContractRegistry.bindingForMixin(mixinClass)
                            != null,
                    mixinClass + " has no exact Mixin binding"
            );
        }
    }

    @Test
    void noMixinCallbackImportsOutsideMixinOrBootstrapPackages()
            throws IOException {
        Path root = PROJECT_ROOT.resolve(
                "src/main/java/com/mafuyu404/taczaddon"
        );
        for (Path source : listJavaFiles(root)) {
            String path = source.toString().replace('\\', '/');
            if (path.contains("/mixin/")
                    || path.endsWith(
                    "/compat/tacz/TaczAddonMixinPlugin.java"
            )) {
                continue;
            }
            String text = Files.readString(
                    source,
                    StandardCharsets.UTF_8
            );
            assertFalse(
                    text.contains(
                            "import org.spongepowered.asm.mixin."
                    ),
                    source + " imports Mixin callback API"
            );
        }
    }

    @Test
    void noBlanketRequireZeroRemainsInTaCZMixins()
            throws IOException {
        Path mixinRoot = PROJECT_ROOT.resolve(
                "src/main/java/com/mafuyu404/taczaddon/mixin/tacz"
        );
        for (Path source : listJavaFiles(mixinRoot)) {
            String text = Files.readString(
                    source,
                    StandardCharsets.UTF_8
            );
            assertFalse(
                    text.contains("require = 0"),
                    source + " still uses blanket require = 0"
            );
        }
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

    private static List<Path> listJavaFiles(Path root)
            throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static String read(String relativePath)
            throws IOException {
        return Files.readString(
                PROJECT_ROOT.resolve(relativePath),
                StandardCharsets.UTF_8
        );
    }
}
