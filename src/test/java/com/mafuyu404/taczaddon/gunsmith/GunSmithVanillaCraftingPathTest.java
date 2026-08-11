package com.mafuyu404.taczaddon.gunsmith;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunSmithVanillaCraftingPathTest {
    private static final Path PROJECT_ROOT =
            Path.of("").toAbsolutePath().normalize();

    private static final List<String> NEARBY_CONTAINER_MIXINS = List.of(
            "GunSmithTableCraftBridgeMixin",
            "GunSmithTableBlockEntityMixin",
            "GunSmithTableMenuAccess",
            "GunSmithTableSourceViewMixin"
    );

    private static final List<String> RESTORED_CLASSES = List.of(
            "client/ClientGunSmithPacketHandler.java",
            "init/GunSmithCraftingSessionManager.java",
            "init/GunSmithCraftingSources.java",
            "init/GunSmithDisplayInventory.java",
            "init/crafting/ContainerItemSource.java",
            "init/crafting/CraftingItemSource.java",
            "init/crafting/CraftingSourceKey.java",
            "init/crafting/CraftingTransaction.java",
            "init/crafting/GunSmithCraftScreenAccess.java",
            "init/crafting/GunSmithSourceScreenAccess.java",
            "init/crafting/PlayerInventorySource.java",
            "mixin/GunSmithTableCraftBridgeMixin.java",
            "mixin/GunSmithTableBlockEntityMixin.java",
            "mixin/GunSmithTableMenuAccess.java",
            "mixin/GunSmithTableSourceViewMixin.java",
            "network/GunSmithCraftRequestPacket.java",
            "network/GunSmithCraftResultPacket.java",
            "network/GunSmithSourceRefreshRequestPacket.java",
            "network/GunSmithSourceSnapshotPacket.java"
    );

    @Test
    void mixinConfigIntentionallyOverridesCraftingAndKeepsBrowseMemory()
            throws IOException {
        String mixins = readProjectFile(
                "src/main/resources/taczaddon.mixins.json"
        );

        for (String mixin : NEARBY_CONTAINER_MIXINS) {
            assertTrue(
                    mixins.contains(mixin),
                    () -> mixin + " must be registered intentionally"
            );
        }

        assertTrue(
                mixins.contains("GunSmithTableBrowseMemoryMixin"),
                "browse memory remains independent of crafting ownership"
        );

        String bridge = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "GunSmithTableCraftBridgeMixin.java"
        );
        assertTrue(bridge.contains("lambda$addCraftButton$5"));
        assertTrue(bridge.contains("require = 1"));
        assertTrue(bridge.contains("new GunSmithCraftRequestPacket("));
        assertTrue(bridge.contains("PENDING_TIMEOUT_MS = 10_000L"));
    }

    @Test
    void clientSnapshotRefreshIsThrottledAndRequestIdSafe()
            throws IOException {
        String sourceView = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "GunSmithTableSourceViewMixin.java"
        );

        assertTrue(sourceView.contains(
                "REFRESH_INTERVAL_TICKS = 30"
        ));
        assertTrue(sourceView.contains("taczaddon$refreshInFlight"));
        assertTrue(sourceView.contains("requestId"));
        assertTrue(sourceView.contains(
                "taczaddon$pendingRefreshRequestId"
        ));
        assertTrue(sourceView.contains("taczaddon$trackedContainerId"));
        assertTrue(sourceView.contains("this.menu.containerId"));

        String containerScreen = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "AbstractContainerScreenMixin.java"
        );
        assertTrue(containerScreen.contains("containerTick"));
        assertTrue(containerScreen.contains("instanceof GunSmithTableScreen"));
    }

    @Test
    void serverCraftStillUsesLiveSourcesAfterClientSnapshot()
            throws IOException {
        String request = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/network/"
                        + "GunSmithCraftRequestPacket.java"
        );
        assertTrue(request.contains("session.validate("));
        assertTrue(request.contains("GunSmithCraftingSources.resolve("));
        assertTrue(request.contains("CraftingTransaction.execute("));

        String transaction = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/crafting/"
                        + "CraftingTransaction.java"
        );
        assertTrue(transaction.contains("player.isCreative()"));
        assertTrue(transaction.contains("transaction.commit()"));
        assertTrue(transaction.contains("spawnOutput(result)"));
    }

    @Test
    void transactionRollbackAndPostCommitSyncAreFailureResilient()
            throws IOException {
        String transaction = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/crafting/"
                        + "CraftingTransaction.java"
        );

        assertTrue(transaction.contains(
                "!transaction.outputSpawned"
        ));
        assertTrue(transaction.contains(
                "insertIntoOtherSlots("
        ));
        assertTrue(transaction.contains(
                "this.player.drop(remainder.copy(), false)"
        ));
        assertTrue(transaction.contains(
                "logSynchronizationFailure("
        ));
        assertTrue(transaction.contains(
                "catch (RuntimeException exception)"
        ));
        assertTrue(transaction.contains(
                "RollbackResult.PARTIALLY_COMPENSATED"
        ));
    }

    @Test
    void containerReaderDisablesOnlyNearbySources() throws IOException {
        String config = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "CommonConfig.java"
        );
        assertTrue(config.contains(
                "gunsmith craft override remains active"
        ));

        String sources = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSources.java"
        );
        assertTrue(sources.contains(
                "if (CommonConfig.enableContainerReader())"
        ));

        String bridge = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "GunSmithTableCraftBridgeMixin.java"
        );
        assertTrue(bridge.contains("new GunSmithCraftRequestPacket("));
    }

    @Test
    void deadRestorationLeftoversAreRemoved() throws IOException {
        String sources = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSources.java"
        );
        String transaction = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/crafting/"
                        + "CraftingTransaction.java"
        );
        String sourceKey = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/crafting/"
                        + "CraftingSourceKey.java"
        );

        assertFalse(sources.contains("ClientFeatureConfigPacketHandler"));
        assertFalse(transaction.contains("FEATURE_DISABLED"));
        assertFalse(transaction.contains("OUT_OF_RANGE"));
        assertFalse(transaction.contains("NO_OUTPUT_SPACE"));
        assertFalse(sourceKey.contains("BackpackPlaced"));
        assertFalse(sourceKey.contains("BackpackEquipped"));
        assertFalse(sourceKey.contains("BackpackCarried"));
    }

    @Test
    void sourceRevisionIsDocumentedAsStructuralOnly()
            throws IOException {
        String session = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSessionManager.java"
        );
        assertTrue(session.contains(
                "Structural/addon-owned mutation revision only"
        ));
        assertTrue(session.contains(
                "re-resolves live server sources"
        ));
    }

    @Test
    void everyDeclaredMixinStillHasASourceClass() throws IOException {
        String mixins = readProjectFile(
                "src/main/resources/taczaddon.mixins.json"
        );
        Pattern arrayEntry = Pattern.compile(
                "(?m)^\\s+\"([A-Za-z0-9_$]+)\"[,]?\\s*$"
        );
        Matcher matcher = arrayEntry.matcher(mixins);
        int declaredCount = 0;

        while (matcher.find()) {
            declaredCount++;
            String className = matcher.group(1);
            Path source = PROJECT_ROOT.resolve(
                    "src/main/java/com/mafuyu404/taczaddon/mixin/"
                            + className
                            + ".java"
            );
            assertTrue(
                    Files.isRegularFile(source),
                    () -> "Declared Mixin has no source class: " + className
            );
        }

        assertEquals(28, declaredCount, "unexpected active Mixin count");
    }

    @Test
    void sessionTransactionAndPacketSourcesAreRestored() {
        Path javaRoot = PROJECT_ROOT.resolve(
                "src/main/java/com/mafuyu404/taczaddon"
        );

        for (String restoredClass : RESTORED_CLASSES) {
            assertTrue(
                    Files.isRegularFile(javaRoot.resolve(restoredClass)),
                    () -> restoredClass + " must be part of the subsystem"
            );
        }
    }

    @Test
    void packetIdsHaveTheirStableSemanticAssignments()
            throws IOException {
        String networkHandler = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/NetworkHandler.java"
        );

        assertTrue(networkHandler.contains("PROTOCOL = \"2.7\""));
        assertPacketId(networkHandler, "ID_SWITCH_GUN", 1);
        assertPacketId(networkHandler, "ID_AMMO_BOX_COLLECT", 2);
        assertPacketId(networkHandler, "ID_SERVER_FEATURE_CONFIG", 3);
        assertPacketId(networkHandler, "ID_GUNSMITH_SOURCE_REFRESH", 4);
        assertPacketId(networkHandler, "ID_GUNSMITH_SOURCE_SNAPSHOT", 5);
        assertPacketId(networkHandler, "ID_GUNSMITH_CRAFT_REQUEST", 6);
        assertPacketId(networkHandler, "ID_GUNSMITH_CRAFT_RESULT", 7);
        assertPacketId(networkHandler, "ID_LIBERATE_ATTACHMENT_STATE", 8);
        assertPacketId(networkHandler, "ID_LIBERATE_ATTACHMENT_INSTALL", 9);

        assertEquals(
                9,
                countOccurrences(networkHandler, "CHANNEL.registerMessage("),
                "all active addon packet types should be registered"
        );
        assertTrue(networkHandler.contains("GunSmithSourceRefreshRequestPacket.class"));
        assertTrue(networkHandler.contains("GunSmithSourceSnapshotPacket.class"));
        assertTrue(networkHandler.contains("GunSmithCraftRequestPacket.class"));
        assertTrue(networkHandler.contains("GunSmithCraftResultPacket.class"));
    }

    @Test
    void addonRequestRetainsTaCZRecipeAuthorization()
            throws IOException {
        String request = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/network/"
                        + "GunSmithCraftRequestPacket.java"
        );

        assertTrue(request.contains("taczaddon$invokeGetRecipe("));
        assertTrue(request.contains("message.recipeId"));
        assertTrue(request.contains("GunSmithCraftingSources.resolve("));
        assertTrue(request.contains("CraftingTransaction.execute("));
    }

    @Test
    void sourceDiscoveryKeepsPlayerFirstAndLoadedDeterministicContainers()
            throws IOException {
        String sources = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSources.java"
        );

        int playerSource = sources.indexOf("new PlayerInventorySource(player)");
        int nearbyScan = sources.indexOf("resolveNearbyContainers(");
        assertTrue(playerSource >= 0 && nearbyScan > playerSource);
        assertTrue(sources.contains("tablePos.offset(-radius, -1, -radius)"));
        assertTrue(sources.contains("tablePos.offset(radius, 1, radius)"));
        assertTrue(sources.contains(
                "Comparator.comparingLong(BlockPos::asLong)"
        ));
        assertTrue(sources.contains("!level.isLoaded(pos)"));
    }

    @Test
    void resolvedTaCZDependencyContainsItsOriginalCraftingChain()
            throws IOException {
        byte[] screen = readClasspathClass(
                "com/tacz/guns/client/gui/GunSmithTableScreen.class"
        );
        byte[] clientMessage = readClasspathClass(
                "com/tacz/guns/network/message/ClientMessageCraft.class"
        );
        byte[] menu = readClasspathClass(
                "com/tacz/guns/inventory/GunSmithTableMenu.class"
        );

        assertClassBytesContain(
                screen,
                "com/tacz/guns/network/message/ClientMessageCraft"
        );
        assertClassBytesContain(screen, "lambda$addCraftButton$5");
        assertClassBytesContain(screen, "getPlayerIngredientCount");
        assertClassBytesContain(screen, "addCraftButton");
        assertClassBytesContain(clientMessage, "GunSmithTableMenu");
        assertClassBytesContain(clientMessage, "doCraft");
        assertClassBytesContain(clientMessage, "encode");
        assertClassBytesContain(clientMessage, "decode");
        assertClassBytesContain(clientMessage, "handle");
        assertClassBytesContain(menu, "getRecipe");
        assertClassBytesContain(menu, "doCraft");
        assertClassBytesContain(menu, "stillValid");
        assertClassBytesContain(
                menu,
                "com/tacz/guns/network/message/ServerMessageCraft"
        );
    }

    private static String readProjectFile(String relativePath)
            throws IOException {
        return Files.readString(
                PROJECT_ROOT.resolve(relativePath),
                StandardCharsets.UTF_8
        );
    }

    private static byte[] readClasspathClass(String resource)
            throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            assertNotNull(
                    stream,
                    "Resolved TaCZ dependency is missing " + resource
            );
            return stream.readAllBytes();
        }
    }

    private static void assertClassBytesContain(
            byte[] classBytes,
            String expectedAscii
    ) {
        String constantPoolView = new String(
                classBytes,
                StandardCharsets.ISO_8859_1
        );
        assertTrue(
                constantPoolView.contains(expectedAscii),
                () -> "Expected dependency class to reference " + expectedAscii
        );
    }

    private static void assertPacketId(
            String source,
            String field,
            int expected
    ) {
        Pattern pattern = Pattern.compile(
                "private\\s+static\\s+final\\s+int\\s+"
                        + Pattern.quote(field)
                        + "\\s*=\\s*(\\d+)\\s*;"
        );
        Matcher matcher = pattern.matcher(source);
        assertTrue(matcher.find(), () -> "Missing explicit packet ID " + field);
        assertEquals(expected, Integer.parseInt(matcher.group(1)), field);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
