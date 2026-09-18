package com.mafuyu404.taczaddon.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Access to the real upstream ABI fixtures resolved by the Gradle
 * {@code resolveCompatibilityFixtures} task.
 *
 * <p>Every fixture is read through the generated
 * {@code build/compatibility-fixtures/fixtures.properties} manifest, so tests
 * never depend on Gradle cache paths or on globbing a directory. A missing
 * entry is a hard failure unless the explicit development property is set (see
 * {@link CompatibilityFixtureGate}).
 */
public final class CompatibilityFixtures {
    public static final Path ROOT = Path.of(
            System.getProperty(
                    "taczaddon.fixtures.root",
                    "build/compatibility-fixtures"
            )
    );

    private static final String MANIFEST_NAME = "fixtures.properties";

    private CompatibilityFixtures() {
    }

    /** e.g. {@code jar("sophisticated", "3.26.0", "backpacks")}. */
    public static Path jar(
            String group,
            String generation,
            String kind
    ) {
        String entry = key(group, generation, kind, "path");
        String relative = entries().get(entry);
        CompatibilityFixtureGate.require(
                relative != null,
                "compatibility fixture manifest is missing " + entry
                        + " in " + ROOT.resolve(MANIFEST_NAME)
                        + " (run gradlew resolveCompatibilityFixtures)"
        );
        Path jar = ROOT.resolve(relative);
        CompatibilityFixtureGate.require(
                Files.isRegularFile(jar),
                "compatibility fixture " + entry + " points at a missing file: "
                        + jar
        );
        return jar;
    }

    public static String artifact(
            String group,
            String generation,
            String kind
    ) {
        return required(
                key(group, generation, kind, "artifact"),
                group + "/" + generation + "/" + kind
        );
    }

    /** Declared upstream version of a fixture, verified against its jar. */
    public static String version(
            String group,
            String generation,
            String kind
    ) {
        return required(
                key(group, generation, kind, "version"),
                group + "/" + generation + "/" + kind
        );
    }

    public static String describe(
            String group,
            String generation,
            String kind
    ) {
        return group + " " + generation + " (" + version(group, generation, kind)
                + " from " + artifact(group, generation, kind) + ")";
    }

    /** Fails when the jar does not identify itself by the pinned version. */
    public static void requireVersion(
            Path jar,
            String expectedVersion,
            String description
    ) {
        String declared = declaredVersion(jar);
        CompatibilityFixtureGate.require(
                declared != null && declared.contains(expectedVersion),
                description + ": expected upstream version " + expectedVersion
                        + " but " + jar.getFileName()
                        + " declares " + declared
        );
    }

    /**
     * Version as the jar declares it: {@code mods.toml} first, then the
     * manifest's {@code Implementation-Version} for mods that use
     * {@code ${file.jarVersion}}.
     */
    public static String declaredVersion(Path jar) {
        String metadata = readEntry(jar, "META-INF/mods.toml");
        if (metadata != null) {
            for (String line : metadata.split("\\R")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("version")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals < 0) {
                    continue;
                }
                String value = trimmed.substring(equals + 1)
                        .replace("#mandatory", "")
                        .trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!value.isBlank() && !value.startsWith("${")) {
                    return value;
                }
            }
        }
        String manifest = readEntry(jar, "META-INF/MANIFEST.MF");
        if (manifest != null) {
            for (String line : manifest.split("\\R")) {
                if (line.startsWith("Implementation-Version:")) {
                    return line.substring(
                            "Implementation-Version:".length()
                    ).trim();
                }
            }
        }
        return null;
    }

    /** Whether the jar's metadata declares a dependency on {@code modId}. */
    public static boolean declaresDependency(Path jar, String modId) {
        return dependencyVersionRange(jar, modId) != null;
    }

    /**
     * The declared dependency version range for {@code modId}, or {@code null}
     * when the jar does not declare that dependency.
     */
    public static String dependencyVersionRange(
            Path jar,
            String modId
    ) {
        String metadata = readEntry(jar, "META-INF/mods.toml");
        if (metadata == null) {
            return null;
        }
        String normalized = metadata.replace("#mandatory", "");
        for (String block : normalized.split("\\[\\[dependencies\\.")) {
            String compact = block.replaceAll("\\s+", "");
            if (!compact.contains("modId=\"" + modId + "\"")) {
                continue;
            }
            int start = compact.indexOf("versionRange=\"");
            if (start < 0) {
                continue;
            }
            int from = start + "versionRange=\"".length();
            int end = compact.indexOf('"', from);
            if (end > from) {
                return compact.substring(from, end);
            }
        }
        return null;
    }

    public static String readEntry(Path jar, String name) {
        try (JarFile file = new JarFile(jar.toFile())) {
            JarEntry entry = file.getJarEntry(name);
            if (entry == null) {
                return null;
            }
            try (InputStream input = file.getInputStream(entry)) {
                return new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }
        } catch (IOException unreadable) {
            return null;
        }
    }

    private static String required(String key, String description) {
        String value = entries().get(key);
        CompatibilityFixtureGate.require(
                value != null,
                "compatibility fixture manifest is missing " + key
                        + " for " + description
        );
        return value;
    }

    private static String key(
            String group,
            String generation,
            String kind,
            String field
    ) {
        return "fixture." + group + "." + generation + "." + kind + "."
                + field;
    }

    private static synchronized Map<String, String> entries() {
        if (CACHE != null) {
            return CACHE;
        }
        Path manifest = ROOT.resolve(MANIFEST_NAME);
        Map<String, String> loaded = new LinkedHashMap<>();
        if (Files.isRegularFile(manifest)) {
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(manifest)) {
                properties.load(input);
            } catch (IOException unreadable) {
                // Left empty; the caller's gate reports the missing entries.
            }
            for (String name : properties.stringPropertyNames()) {
                loaded.put(name, properties.getProperty(name));
            }
        }
        CACHE = Map.copyOf(loaded);
        return CACHE;
    }

    private static volatile Map<String, String> CACHE;
}
