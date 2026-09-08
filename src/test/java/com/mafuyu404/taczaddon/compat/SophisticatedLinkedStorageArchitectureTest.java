package com.mafuyu404.taczaddon.compat;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SophisticatedLinkedStorageArchitectureTest {
    private static final Path COMPAT = Path.of("src/main/java/com/mafuyu404/taczaddon/compat");

    private static String source(String name) throws Exception {
        return Files.readString(COMPAT.resolve(name + ".java"));
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature);
        int open = source.indexOf('{', start);
        int depth = 1;
        int end = open + 1;
        while (depth > 0) {
            char c = source.charAt(end++);
            if (c == '{') depth++;
            if (c == '}') depth--;
        }
        return source.substring(open, end);
    }

    @Test
    void newApiReferencesAreIsolatedAndOldGenerationLoadsSilently() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                for (String forbidden : new String[] {"RequestLinkedStorageBackpackContentsPayload",
                        "LinkedStorageBackpackContentsPayload", "ModCoreDataComponents.LINKED_STORAGE_ENDPOINT",
                        "PacketDistributor.sendToServer", "RegistryFriendlyByteBuf"}) {
                    assertFalse(text.contains(forbidden), file.toString());
                }
                if (file.getFileName().toString().equals("SophisticatedLinkedStorageCompat326.java")) continue;
                for (String line : text.lines().filter(l -> l.startsWith("import ")).toList()) {
                    assertFalse(line.contains("sophisticatedcore.linkedstorage"), file.toString());
                    assertFalse(line.contains("LinkedStorageBackpackContentsMessage"), file.toString());
                }
            }
        }
        String facade = source("SophisticatedLinkedStorageCompat");
        assertTrue(facade.contains("Class.forName(IMPL_CLASS, true, loader)"));
        assertTrue(facade.contains("Class.forName(REQUIRED_CLASS, false, loader)"));
        String absent = facade.substring(facade.indexOf("catch (ClassNotFoundException absent)"),
                facade.indexOf("catch (LinkageError error)", facade.indexOf("catch (ClassNotFoundException absent)")));
        assertTrue(absent.contains("return NoopBridge.INSTANCE"));
        assertFalse(absent.contains("logBroken"));
        assertFalse(facade.contains("breakLinkage"));
        assertFalse(facade.contains("catch (Throwable"));
        for (String signature : new String[] {"static EndpointResolution resolve(",
                "static void requestSnapshot(", "static void sendSnapshot("}) {
            assertTrue(method(facade, signature).contains("catch (LinkageError error)"));
        }
    }

    @Test
    void bootstrapDeduplicatesIndependentNamespacesAndMalformedEndpointsCannotFallThrough() throws Exception {
        String sync = method(source("SophisticatedBackpacksCompatInner"), "public static void syncAllBackpack(");
        assertTrue(sync.contains("Set<UUID> requestedStorageUuids = new HashSet<>()"));
        assertTrue(sync.contains("Set<UUID> requestedLinkedGroups = new HashSet<>()"));
        assertTrue(sync.contains("(backpack, handlerName, identifier, slot) ->"));
        assertTrue(sync.indexOf("resolve(backpack)") < sync.indexOf("new BackpackContext.Item("));
        String linked = method(sync, "if (linked.linked())");
        assertTrue(linked.contains("groupIdOptional()"));
        assertTrue(linked.contains(".filter(requestedLinkedGroups::add)"));
        assertTrue(linked.contains(".ifPresent(SophisticatedLinkedStorageCompat::requestSnapshot)"));
        assertTrue(linked.contains("return false;"));
        assertFalse(linked.contains("RequestBackpackInventoryContentsMessage"));
        String ordinary = sync.substring(sync.indexOf("new BackpackContext.Item("));
        assertTrue(ordinary.contains(".filter(requestedStorageUuids::add)"));
        assertTrue(ordinary.contains("new RequestBackpackInventoryContentsMessage(uuid)"));
        assertFalse(ordinary.contains("requestSnapshot"));
        String impl = source("SophisticatedLinkedStorageCompat326");
        assertTrue(impl.contains("new RequestLinkedStorageBackpackContentsMessage(groupId, -1L)"));
        assertTrue(impl.contains("endpoint == null || endpoint.groupId() == null"));
        assertTrue(impl.contains("EndpointResolution.malformedLinked()"));
    }

    @Test
    void mutationAndFreshnessUseOnlyTheirOwnStorageFamily() throws Exception {
        String inner = source("SophisticatedBackpacksCompatInner");
        String mutation = method(inner, "private static void syncBackpackContents(");
        String linked = method(mutation, "if (linked.linked())");
        assertTrue(linked.contains("groupIdOptional().ifPresent"));
        assertTrue(linked.contains("SophisticatedLinkedStorageCompat.sendSnapshot(player, groupId)"));
        assertTrue(linked.contains("return;"));
        assertFalse(linked.contains("new BackpackContentsMessage"));
        assertTrue(mutation.indexOf("return;") < mutation.indexOf("wrapper.getContentsUuid()"));
        assertTrue(mutation.contains("new BackpackContentsMessage("));
        assertTrue(source("SophisticatedLinkedStorageCompat326").contains(
                "LinkedStorageBackpackContentsMessage.createSnapshot(player.serverLevel(), groupId)"));
        String freshness = method(inner, "private static InventoryHandler getFreshInventoryHandler(");
        assertTrue(freshness.contains("resolve(wrapper.getBackpack())"));
        assertTrue(method(freshness, "if (linked.linked())").contains("return handler;"));
        assertTrue(freshness.indexOf("if (linked.linked())") < freshness.indexOf("wrapper.getContentsUuid()"));
        assertTrue(freshness.contains("needsClientInventoryRefresh("));
        assertTrue(freshness.contains("getOrCreateBackpackContents(uuid)"));
    }

    @Test
    void metadataAndMixinMatchJava17Forge1201() throws Exception {
        String props = Files.readString(Path.of("gradle.properties"));
        for (String declaration : new String[] {"minecraft_version_range=[1.20.1]",
                "forge_version_range=[47.4.20,48)", "loader_version_range=[47,48)",
                "mod_version=1.1.8.2"}) assertTrue(props.contains(declaration), declaration);
        String mods = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        assertTrue(mods.contains("versionRange=\"[1.1.8-hotfix]\""));
        for (String config : new String[] {"taczaddon.mixins.json", "taczaddon.tacz.mixins.json"}) {
            assertTrue(Files.readString(Path.of("src/main/resources", config))
                    .contains("\"compatibilityLevel\": \"JAVA_17\""));
        }
    }
}
