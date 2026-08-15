package com.mafuyu404.taczaddon.compat.tacz;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void mixinConfigsAreSplitAndUsePlatformCompatibility()
            throws IOException {
        String generic = read(
                "src/main/resources/taczaddon.mixins.json"
        );
        String tacz = read(
                "src/main/resources/taczaddon.tacz.mixins.json"
        );

        assertFalse(generic.contains("JAVA_17"));
        assertFalse(tacz.contains("JAVA_17"));
        assertTrue(tacz.contains(
                "\"plugin\": \"com.mafuyu404.taczaddon.compat.tacz."
                        + "TaczAddonMixinPlugin\""
        ));
        assertTrue(generic.contains("SmithingMenuMixin"));
        assertTrue(generic.contains("AbstractContainerScreenMixin"));
        assertTrue(tacz.contains("v1_1_8.LocalPlayerDrawMixin"));
        assertTrue(tacz.contains("v1_1_8.GunSmithTableSourceViewMixin"));
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
        String tacz = read(
                "src/main/resources/taczaddon.tacz.mixins.json"
        );
        for (String entry : entries(tacz)) {
            String mixinClass = "com.mafuyu404.taczaddon.mixin.tacz."
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
