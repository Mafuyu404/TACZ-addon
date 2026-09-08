package com.mafuyu404.taczaddon.init;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mafuyu404.taczaddon.testutil.MinecraftTestBootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class AttachmentDetailRuleTest {
    private static final String SELF = Type.getInternalName(AttachmentDetailRuleTest.class);
    private static final String REPLAY = "com/mafuyu404/taczaddon/testutil/RuleReplay";
    private static final List<Boolean> sent = new ArrayList<>();
    public static List<ServerPlayer> players(MinecraftServer server) { return Arrays.asList(null, null, null); }
    public static void send(ServerPlayer player, boolean value) { sent.add(value); }

    @BeforeAll static void prepare() throws Exception { MinecraftTestBootstrap.prepare(); }
    @AfterEach void reset() { ClientSyncedConfig.resetToSafeDefaults(); }

    @Test void registeredRuleDefaultsToFalse() {
        RuleRegistry.bootstrap();
        assertFalse(new GameRules().getBoolean(RuleRegistry.SHOW_ATTACHMENT_DETAIL));
    }

    @Test void actualTooltipEntryGateRequiresBothClientPreferenceAndSyncedRule() throws Exception {
        ClassNode original = read("com/mafuyu404/taczaddon/mixin/tacz/v1_1_8/ClientAttachmentItemTooltipMixin");
        MethodNode gate = original.methods.stream().filter(m -> m.name.equals("taczaddon$appendAttributeDifferences")).findFirst().orElseThrow();
        // Stop immediately after the real entry guard, before accessing the rendered attachment.
        var field = Arrays.stream(gate.instructions.toArray()).filter(i -> i instanceof FieldInsnNode f && f.name.equals("attachment")).findFirst().orElseThrow();
        var cut = field.getPrevious();
        while (cut != null) { var next = cut.getNext(); gate.instructions.remove(cut); cut = next; }
        for (var insn : gate.instructions.toArray()) if (insn.getOpcode() == Opcodes.RETURN) {
            gate.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_0));
            gate.instructions.set(insn, new InsnNode(Opcodes.IRETURN));
        }
        gate.instructions.add(new InsnNode(Opcodes.ICONST_1)); gate.instructions.add(new InsnNode(Opcodes.IRETURN));
        gate.name = "enabled"; gate.desc = "()Z"; gate.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        Method enabled = define(List.of(gate)).getMethod("enabled");
        Config.SPEC.setConfig(CommentedConfig.inMemory());
        for (boolean server : new boolean[]{false, true}) for (boolean client : new boolean[]{false, true}) {
            ClientSyncedConfig.setShowAttachmentDetail(server); Config.SHOW_ATTACHMENT_ATTRIBUTE.set(client);
            assertEquals(server && client, enabled.invoke(null), "server=" + server + ",client=" + client);
        }
        Config.SHOW_ATTACHMENT_ATTRIBUTE.set(true);
    }

    @Test void ruleCallbackBroadcastsCurrentValueToEveryConnectedPlayer() throws Exception {
        ClassNode original = read("com/mafuyu404/taczaddon/init/RuleRegistry");
        List<MethodNode> methods = original.methods.stream().filter(m -> m.name.contains("onShowAttachmentDetailChanged")).toList();
        assertEquals(2, methods.size());
        for (MethodNode method : methods) {
            method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            for (var insn : method.instructions.toArray()) {
                if (insn instanceof MethodInsnNode call && call.name.equals("getPlayerList")) {
                    call.setOpcode(Opcodes.INVOKESTATIC); call.owner = SELF; call.name = "players";
                    call.desc = "(Lnet/minecraft/server/MinecraftServer;)Ljava/util/List;";
                } else if (insn instanceof MethodInsnNode call && call.name.equals("getPlayers")) {
                    method.instructions.remove(insn);
                } else if (insn instanceof MethodInsnNode call && call.name.equals("sendAttachmentDetailRuleState")) {
                    call.owner = SELF; call.name = "send";
                } else if (insn instanceof InvokeDynamicInsnNode dynamic) {
                    for (int i = 0; i < dynamic.bsmArgs.length; i++) if (dynamic.bsmArgs[i] instanceof Handle handle
                            && handle.getOwner().equals(original.name)) dynamic.bsmArgs[i] = new Handle(handle.getTag(), REPLAY,
                                    handle.getName(), handle.getDesc(), handle.isInterface());
                }
            }
        }
        Method callback = define(methods).getMethod("onShowAttachmentDetailChanged", MinecraftServer.class, GameRules.BooleanValue.class);
        var rule = new GameRules().getRule(RuleRegistry.SHOW_ATTACHMENT_DETAIL);
        for (boolean value : new boolean[]{true, false}) {
            sent.clear(); rule.set(value, null); callback.invoke(null, null, rule);
            assertEquals(List.of(value, value, value), sent);
        }
    }

    @Test void lifecycleAndPacketKeepForgeSynchronizationAndBothTooltipGates() throws Exception {
        Path root = Path.of("src/main/java/com/mafuyu404/taczaddon");
        String events = Files.readString(root.resolve("event/ServerEvent.java"));
        for (String method : new String[]{"onPlayerLoggedIn", "onPlayerChangedDimension", "onPlayerRespawn"}) {
            int start = events.indexOf("void " + method);
            int end = events.indexOf("@SubscribeEvent", start);
            String body = events.substring(start, end < 0 ? events.length() : end);
            assertTrue(body.contains("sendLiberateAttachmentState(serverPlayer)"));
            assertTrue(body.contains("sendAttachmentDetailRuleState(serverPlayer)"));
        }
        String tooltip = Files.readString(root.resolve("mixin/tacz/v1_1_8/ClientAttachmentItemTooltipMixin.java"));
        assertTrue(tooltip.contains("!Config.SHOW_ATTACHMENT_ATTRIBUTE.get()"));
        assertTrue(tooltip.contains("|| !ClientSyncedConfig.showAttachmentDetail()"));
        String rule = Files.readString(root.resolve("init/RuleRegistry.java"));
        assertTrue(rule.contains("GameRules.Category.PLAYER"));
        String packet = Files.readString(root.resolve("network/AttachmentDetailRuleStatePacket.java"));
        for (String token : new String[]{"FriendlyByteBuf", "enqueueWork", "DistExecutor", "setPacketHandled(true)"}) assertTrue(packet.contains(token));
        assertTrue(Files.readString(root.resolve("event/ClientSyncedConfigEvents.java")).contains("resetToSafeDefaults()"));
    }

    private static ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = AttachmentDetailRuleTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input); new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }
    private static Class<?> define(List<MethodNode> methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, REPLAY, null, "java/lang/Object", null);
        for (MethodNode method : methods) {
            method.visibleAnnotations = null; method.invisibleAnnotations = null;
            method.visibleParameterAnnotations = null; method.invisibleParameterAnnotations = null;
            method.accept(writer);
        }
        writer.visitEnd(); byte[] bytes = writer.toByteArray();
        return new ClassLoader(AttachmentDetailRuleTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(REPLAY.replace('/', '.'), bytes, 0, bytes.length); }
        }.define();
    }
}
