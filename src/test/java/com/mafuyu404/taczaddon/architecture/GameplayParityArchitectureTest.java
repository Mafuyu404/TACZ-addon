package com.mafuyu404.taczaddon.architecture;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks actual supported TaCZ bytecode as well as the addon injection boundaries. */
class GameplayParityArchitectureTest {
    private static final String CORE_SHOOT = "(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;";
    private static ClassNode type(String name) throws Exception {
        try (var input = GameplayParityArchitectureTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            ClassNode node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    private static MethodNode method(ClassNode type, String name, String descriptor) {
        return type.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor)).findFirst().orElseThrow();
    }
    private static String source(String name) throws Exception {
        return Files.readString(com.mafuyu404.taczaddon.testutil.SourceTree.path("src/main/java/com/mafuyu404/taczaddon/" + name + ".java"));
    }
    private static int count(String text, String needle) { return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1; }
    @Test void requiredServerMixinsApplyToTheInstalledDependency() throws Exception {
        for (var contract : Map.of(
                "com.tacz.guns.entity.shooter.LivingEntityShoot", "taczaddon$commitReloadInterruption",
                "com.tacz.guns.inventory.GunSmithTableMenu", "taczaddon$bridgeCraft"
        ).entrySet()) {
            Class<?> target = Class.forName(contract.getKey(), false, getClass().getClassLoader());
            assertTrue(Arrays.stream(target.getDeclaredMethods()).anyMatch(method -> method.getName().contains(contract.getValue())),
                    contract.getKey() + " required mixin did not apply");
        }
    }
    @Test void serverCommitPointFollowsAllNativeRejections() throws Exception {
        var type = type("com/tacz/guns/entity/shooter/LivingEntityShoot");
        var shot = method(type, "shoot", CORE_SHOOT);
        int commit = -1, reloadChecks = 0, eventCheck = -1, rejections = 0;
        for (int i = 0; i < shot.instructions.size(); i++) {
            var insn = shot.instructions.get(i);
            if (insn instanceof TypeInsnNode allocation && allocation.getOpcode() == Opcodes.NEW
                    && allocation.desc.equals("com/tacz/guns/network/message/event/ServerMessageGunShoot")) commit = i;
            if (insn instanceof MethodInsnNode call && call.owner.equals("com/tacz/guns/api/entity/ReloadState$StateType")
                    && call.name.equals("isReloading")) reloadChecks++;
            if (insn instanceof MethodInsnNode call && call.owner.equals("com/tacz/guns/api/event/common/GunShootEvent")
                    && call.name.equals("isCanceled")) eventCheck = i;
            if (insn instanceof FieldInsnNode field && field.owner.equals("com/tacz/guns/api/entity/ShootResult")
                    && !field.name.equals("SUCCESS")) {
                assertEquals(-1, commit, "Native failure follows the selected commit point: " + field.name); rejections++;
            }
        }
        assertEquals(1, reloadChecks); assertTrue(rejections >= 10);
        assertTrue(eventCheck >= 0 && commit > eventCheck);
        var wrapper = method(type, "shoot", "(Ljava/util/function/Supplier;Ljava/util/function/Supplier;J)Lcom/tacz/guns/api/entity/ShootResult;");
        assertTrue(Arrays.stream(wrapper.instructions.toArray()).anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("shoot") && call.desc.equals(CORE_SHOOT)));
        String addon = source("mixin/LivingEntityShootMixin");
        assertTrue(addon.contains(CORE_SHOOT));
        assertEquals(1, count(addon, "operator.cancelReload()"));
        assertTrue(addon.indexOf("taczaddon$commitReloadInterruption") < addon.indexOf("operator.cancelReload()"));
        String head = addon.substring(addon.indexOf("taczaddon$beforeShot"), addon.indexOf("@Redirect"));
        assertFalse(head.contains("cancelReload")); assertFalse(head.contains("reloadTimestamp ="));
        assertTrue(addon.contains("@At(\"RETURN\")")); assertFalse(addon.contains("require = 0"));
    }
    @Test void clientHooksMatchNativeLockAndReloadGates() throws Exception {
        var type = type("com/tacz/guns/client/gameplay/LocalPlayerShoot");
        var shoot = method(type, "shoot", "()Lcom/tacz/guns/api/entity/ShootResult;");
        long locks = Arrays.stream(shoot.instructions.toArray()).filter(i -> i instanceof FieldInsnNode field
                && field.name.equals("clientStateLock") && field.getOpcode() == Opcodes.GETFIELD).count();
        assertEquals(1, locks);
        long commits = Arrays.stream(shoot.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                && call.name.equals("lockState") && call.desc.equals("(Ljava/util/function/Predicate;)V")).count();
        assertEquals(1, commits);
        var preCheck = type.methods.stream().filter(m -> m.name.equals("preCheck")).findFirst().orElseThrow();
        assertEquals(1, Arrays.stream(preCheck.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                && call.owner.equals("com/tacz/guns/api/entity/ReloadState$StateType") && call.name.equals("isReloading")).count());
        String service = source("client/ShootWhenReload");
        assertFalse(service.contains("sendToServer")); assertFalse(service.contains("operator.cancelReload()"));
        assertFalse(service.contains("hasInventoryAmmo(")); assertTrue(service.contains("ClientSyncedConfig.enableShootWhileReloading()"));
        assertTrue(service.contains("GunAnimationConstant.INPUT_CANCEL_RELOAD"));
        assertTrue(service.contains("normalized.startsWith(\"reload\")")); assertTrue(service.contains("normalized.contains(\"_reload\")"));
        String mixin = source("mixin/LocalPlayerShootMixin");
        assertTrue(mixin.contains("BetterMelee.onShoot(cir)")); assertTrue(mixin.contains("slideShoot"));
        assertTrue(mixin.contains("@At(\"RETURN\")")); assertFalse(mixin.contains("require = 0"));
    }
    @Test void craftingUsesOneRequestAndServerConfirmedResult() throws Exception {
        String ui = source("mixin/GunSmithTableScreenMixin");
        assertFalse(ui.contains("original.onPress")); assertFalse(ui.contains("Craft requested"));
        String callback = ui.substring(ui.indexOf("private Button.OnPress taczaddon$wrapCraftButtonCallback"));
        assertEquals(1, count(callback, "GunSmithCraftBridgeState.request(")); assertFalse(callback.contains("for ("));
        assertTrue(callback.contains("this.selectedRecipe.id()"));
        var bridge = source("client/GunSmithCraftBridgeState");
        assertTrue(bridge.contains("ClientSyncedConfig.getBatchCraftMax() : 1"));
        assertTrue(bridge.contains("result.craftedExecutions()")); assertTrue(bridge.contains("pending.remove(result.requestId())"));
        String request = source("network/GunSmithCraftRequestPacket");
        for (String guard : List.of("ServerboundPacketGuard.isRateLimited", "GunSmithTableMenu", "session.validate", "acceptCraftRequestId",
                "taczaddon$invokeGetRecipe", "Config.getBatchCraftMax()", "CraftingTransaction.execute")) assertTrue(request.contains(guard), guard);
        assertTrue(request.indexOf("if (!result.success())") < request.indexOf("craftedExecutions++"));
        assertTrue(source("mixin/GunSmithTableMenuMixin").contains("ci.cancel()"));
    }
    @Test void nearbySourcesRemainLoadedOnlyAndUseOptionalFacade() throws Exception {
        String resolver = source("init/NearbyInventorySourceResolver").replaceAll("\\s*\\.\\s*", ".");
        assertFalse(resolver.contains(".getChunk("));
        assertTrue(resolver.indexOf("isLoaded(pos)") < resolver.indexOf("getBlockEntity(pos)"));
        assertTrue(resolver.contains("ContainerMaster.getContainerHandler"));
        assertTrue(resolver.contains("SophisticatedBackpacksCompat.forEachBlockBackpackHandler"));
        assertTrue(resolver.contains("IdentityHashMap"));
        for (String name : List.of("common/RefitSourceResolver", "init/GunSmithCraftingSources", "network/ContainerPositionPacket")) {
            assertTrue(source(name).contains("NearbyInventorySourceResolver.resolve"), name);
            assertFalse(source(name).contains("import net.p3pp3rf1y."), name);
        }
    }
    @Test void externalRefitUsesServerLocatorValidationAndSafeReturnPreflight() throws Exception {
        String resolver = source("common/RefitSourceResolver");
        assertTrue(resolver.contains("!LiberateAttachment.isLiberated(player)"));
        String service = source("common/RefitExternalInstallService");
        for (String guard : List.of("RefitSourceResolver.canUseSources", "gunSlot != player.getInventory().selected", "locator.dimension()",
                "inRange", "isLoaded(locator.pos())", "NearbyInventorySourceResolver.resolve", "source.isValid()", "locator.slot() >= handler.getSlots()",
                "expectedId.equals(attachment.getAttachmentId(current))", "expectedType != attachment.getType(current)", "gun.allowAttachment(gunStack, current)")) {
            assertTrue(service.contains(guard), guard);
        }
        String transaction = source("common/AttachmentRefitService");
        transaction = transaction.substring(transaction.indexOf("public static InstallResult installExternal"), transaction.indexOf("public enum UnloadResult"));
        assertTrue(transaction.indexOf("extraction.simulateOne()") < transaction.indexOf("transaction.canFullyInsert"));
        assertTrue(transaction.indexOf("transaction.canFullyInsert") < transaction.indexOf("extraction.extractOne()"));
        assertTrue(transaction.contains("extraction.rollback()")); assertTrue(transaction.contains("handleRollbackOutcome"));
        assertTrue(transaction.indexOf("transaction.close();", transaction.indexOf("gun.installAttachment")) < transaction.indexOf("postChange("));
        assertFalse(transaction.contains("player.drop("));
        String ui = source("mixin/GunRefitScreenMixin");
        assertTrue(ui.contains("RefitExternalSourceState.createDisplayInventory"));
        assertTrue(ui.contains("RefitExternalAttachmentInstallPacket"));
        assertTrue(ui.contains("LiberateAttachment.isLiberated(minecraft.player)"));
        assertTrue(ui.contains("TACZADDON_ATTACHMENTS_PER_PAGE = 8"));
    }
    @Test void existingPropertyFilterAndSecurityGuardsRemain() throws Exception {
        String smith = source("mixin/GunSmithTableScreenMixin");
        for (String property : List.of("taczaddon$attachmentProp", "taczaddon$selectedAttachmentPropIndex", "DropDown", "classifyRecipes", "browseStateRestored"))
            assertTrue(smith.contains(property), property);
        assertTrue(source("network/SwitchGunPacket").contains("ServerboundPacketGuard"));
        assertTrue(source("network/AmmoBoxCollectPacket").contains("ServerboundPacketGuard"));
        assertTrue(source("mixin/TaczAddonMixinPlugin").contains("SophisticatedPayloadContractState"));
        for (String packet : List.of("GunSmithCraftRequestPacket", "RefitSourceRefreshRequestPacket", "RefitExternalAttachmentInstallPacket")) {
            String text = source("network/" + packet);
            assertTrue(text.contains("ServerboundPacketGuard.isRateLimited"));
            assertFalse(text.contains("ItemStack.STREAM_CODEC.decode"));
            assertFalse(text.contains("net.minecraftforge"));
        }
    }
}
