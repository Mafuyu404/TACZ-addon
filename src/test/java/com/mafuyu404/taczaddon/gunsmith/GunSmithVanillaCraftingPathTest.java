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
            "v1_1_8.GunSmithTableCraftBridgeMixin",
            "v1_1_8.GunSmithTableMenuAccess",
            "v1_1_8.GunSmithTableSourceViewMixin"
    );

    private static final List<String> RESTORED_CLASSES = List.of(
            "client/ClientGunSmithPacketHandler.java",
            "client/GunSmithCraftBridgeState.java",
            "client/GunSmithExternalSourceState.java",
            "client/GunSmithCompatibilityService.java",
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
            "mixin/tacz/v1_1_8/GunSmithTableCraftBridgeMixin.java",
            "mixin/tacz/v1_1_8/GunSmithTableMenuAccess.java",
            "mixin/tacz/v1_1_8/GunSmithTableSourceViewMixin.java",
            "network/GunSmithCraftRequestPacket.java",
            "network/GunSmithCraftResultPacket.java",
            "network/GunSmithSourceRefreshRequestPacket.java",
            "network/GunSmithSourceSnapshotPacket.java"
    );

    @Test
    void forgeEventsReplaceCreateMenuMixinAndKeepBrowseMemory()
            throws IOException {
        String mixins = readProjectFile(
                "src/main/resources/taczaddon.tacz.mixins.json"
        );
        String removedMixin = "GunSmithTable" + "BlockEntityMixin";

        assertFalse(
                mixins.contains(removedMixin),
                "create menu Mixin must not remain registered"
        );

        for (String mixin : NEARBY_CONTAINER_MIXINS) {
            assertTrue(
                    mixins.contains(mixin),
                    () -> mixin + " must be registered intentionally"
            );
        }

        assertTrue(
                mixins.contains("v1_1_8.GunSmithTableBrowseMemoryMixin"),
                "browse memory remains independent of crafting ownership"
        );

        String bridge = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "tacz/v1_1_8/"
                        + "GunSmithTableCraftBridgeMixin.java"
        );
        assertFalse(bridge.contains("lambda$addCraftButton$5"));
        assertTrue(bridge.contains("method = \"addCraftButton()V\""));
        assertTrue(bridge.contains("taczaddon$wrapCraftButton"));
        assertTrue(bridge.contains("require = 1"));
        assertTrue(bridge.contains("GunSmithCraftBridgeState"));
    }

    @Test
    void browseMemoryRestoresOnceAndKeepsPagesIndependent()
            throws IOException {
        String browseMixin = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "tacz/v1_1_8/"
                        + "GunSmithTableBrowseMemoryMixin.java"
        );
        assertFalse(browseMixin.contains("taczaddon$saveBeforeReinit"));
        assertFalse(browseMixin.contains("taczaddon$browseStateRestored"));
        assertTrue(browseMixin.contains(
                "taczaddon$initialBrowseRestoreAttempted"
        ));
        assertTrue(browseMixin.contains(
                "if (this.taczaddon$initialBrowseRestoreAttempted)"
        ));
        assertTrue(browseMixin.contains(
                "this.taczaddon$initialBrowseRestoreAttempted = true"
        ));

        String service = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/client/"
                        + "GunSmithCompatibilityService.java"
        );
        assertFalse(service.contains("typePageFor"));
        assertFalse(service.contains("savedRecipeIndex / 6"));
    }

    @Test
    void gunsmithSessionUsesServerEventCorrelationWithoutCreateMenuMixin()
            throws IOException {
        String manager = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSessionManager.java"
        );
        String serverEvent = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/event/"
                        + "ServerEvent.java"
        );
        String removedInjection = "taczaddon$" + "createCraftingSession";
        String productionMenuMethod = "m_" + "7208_";
        String developmentMenuMethod = "create" + "Menu";

        assertFalse(manager.contains(removedInjection));
        assertFalse(manager.contains(productionMenuMethod));
        assertFalse(manager.contains(developmentMenuMethod));

        assertTrue(serverEvent.contains(
                "PlayerInteractEvent.RightClickBlock"
        ));
        assertTrue(serverEvent.contains("AbstractGunSmithTableBlock"));
        assertTrue(serverEvent.contains("getRootPos("));
        assertTrue(serverEvent.contains("EventPriority.LOWEST"));
        assertTrue(serverEvent.contains("receiveCanceled = false"));
        assertTrue(serverEvent.contains("rememberTableInteraction("));
        assertTrue(serverEvent.contains("PlayerContainerEvent.Open"));
        assertTrue(serverEvent.contains("createSessionFromPending("));
        assertTrue(serverEvent.contains("clearPlayerState("));
    }

    @Test
    void clientSnapshotRefreshIsThrottledAndRequestIdSafe()
            throws IOException {
        String state = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/client/"
                        + "GunSmithExternalSourceState.java"
        );

        assertTrue(state.contains("REFRESH_INTERVAL_TICKS = 30"));
        assertTrue(state.contains("refreshInFlight"));
        assertTrue(state.contains("requestId"));
        assertTrue(state.contains("pendingRefreshRequestId"));
        assertTrue(state.contains("trackedContainerId"));

        String sourceView = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "tacz/v1_1_8/"
                        + "GunSmithTableSourceViewMixin.java"
        );
        assertTrue(sourceView.contains("getPlayerIngredientCount("));
        assertFalse(sourceView.contains("@ModifyVariable"));
        assertFalse(sourceView.contains("@At(\"STORE\")"));

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
        assertTrue(transaction.contains("insertIntoOtherSlots("));
        assertTrue(transaction.contains(
                "this.player.drop(remainder.copy(), false)"
        ));
        assertTrue(transaction.contains("logSynchronizationFailure("));
        assertTrue(transaction.contains("catch (RuntimeException exception)"));
        assertTrue(transaction.contains("RollbackResult.PARTIALLY_COMPENSATED"));
    }

    @Test
    void containerReaderDisablesOnlyNearbySources() throws IOException {
        String config = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "CommonConfig.java"
        );
        assertTrue(config.contains(
                "enableNearbyContainerSources"
        ));
        assertTrue(config.contains(
                "nearby loaded block inventories"
        ));

        String sources = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "GunSmithCraftingSources.java"
        );
        assertTrue(sources.contains(
                "if (CommonConfig.enableContainerReader())"
        ));
        assertTrue(sources.contains(
                "NearbyInventorySourceResolver.resolve"
        ));

        String bridge = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/mixin/"
                        + "tacz/v1_1_8/"
                        + "GunSmithTableCraftBridgeMixin.java"
        );
        assertTrue(bridge.contains("GunSmithCraftBridgeState"));
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
        assertTrue(session.contains("re-resolves live server sources"));
    }

    @Test
    void everyDeclaredMixinStillHasASourceClass() throws IOException {
        String generic = readProjectFile(
                "src/main/resources/taczaddon.mixins.json"
        );
        String tacz = readProjectFile(
                "src/main/resources/taczaddon.tacz.mixins.json"
        );
        int count = assertEntriesHaveSources(
                generic,
                "com.mafuyu404.taczaddon.mixin"
        );
        count += assertEntriesHaveSources(
                tacz,
                "com.mafuyu404.taczaddon.mixin.tacz"
        );
        assertTrue(count >= 28, "expected split Mixin configurations");
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
                "src/main/java/com/mafuyu404/taczaddon/init/"
                        + "NetworkHandler.java"
        );

        assertTrue(networkHandler.contains("PROTOCOL = \"2.8\""));
        assertPacketId(networkHandler, "ID_SWITCH_GUN", 1);
        assertPacketId(networkHandler, "ID_AMMO_BOX_COLLECT", 2);
        assertPacketId(networkHandler, "ID_SERVER_FEATURE_CONFIG", 3);
        assertPacketId(networkHandler, "ID_GUNSMITH_SOURCE_REFRESH", 4);
        assertPacketId(networkHandler, "ID_GUNSMITH_SOURCE_SNAPSHOT", 5);
        assertPacketId(networkHandler, "ID_GUNSMITH_CRAFT_REQUEST", 6);
        assertPacketId(networkHandler, "ID_GUNSMITH_CRAFT_RESULT", 7);
        assertPacketId(networkHandler, "ID_LIBERATE_ATTACHMENT_STATE", 8);
        assertPacketId(networkHandler, "ID_LIBERATE_ATTACHMENT_INSTALL", 9);
        assertPacketId(networkHandler, "ID_REFIT_SOURCE_REFRESH", 10);
        assertPacketId(networkHandler, "ID_REFIT_SOURCE_SNAPSHOT", 11);
        assertPacketId(networkHandler, "ID_REFIT_EXTERNAL_INSTALL", 12);

        assertEquals(
                12,
                countOccurrences(networkHandler, "CHANNEL.registerMessage("),
                "all active addon packet types should be registered"
        );
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
        int nearbyScan = sources.indexOf(
                "NearbyInventorySourceResolver.resolve"
        );
        assertTrue(playerSource >= 0 && nearbyScan > playerSource);

        String resolver = readProjectFile(
                "src/main/java/com/mafuyu404/taczaddon/init/crafting/"
                        + "NearbyInventorySourceResolver.java"
        );
        assertTrue(resolver.contains("BlockPos.betweenClosed(min, max)"));
        assertTrue(resolver.contains(
                "Comparator.comparingLong(BlockPos::asLong)"
        ));
        assertTrue(resolver.contains("!level.isLoaded(pos)"));
        assertFalse(sources.contains("BlockPos.betweenClosed"));
        assertFalse(sources.contains("resolveNearbyContainers"));
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

    private static int assertEntriesHaveSources(
            String mixins,
            String packageName
    ) throws IOException {
        Pattern arrayEntry = Pattern.compile(
                "(?m)^\\s+\"([A-Za-z0-9_$.]+)\"[,]?\\s*$"
        );
        Matcher matcher = arrayEntry.matcher(mixins);
        int count = 0;
        while (matcher.find()) {
            count++;
            String className = matcher.group(1);
            String relative = packageName.replace('.', '/')
                    + "/"
                    + className.replace('.', '/')
                    + ".java";
            assertTrue(
                    Files.isRegularFile(
                            PROJECT_ROOT.resolve(
                                    "src/main/java/" + relative
                            )
                    ),
                    () -> "Declared Mixin has no source class: "
                            + packageName
                            + "."
                            + className
            );
        }
        return count;
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
