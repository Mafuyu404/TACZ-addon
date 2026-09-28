package com.mafuyu404.taczaddon.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SophisticatedLinkedStorageNetworkTest {
    private static final String ADAPTER = SophisticatedLinkedStorageCompatNetwork.class.getName();

    @TempDir Path temporary;

    @ParameterizedTest
    @EnumSource(SophisticatedLinkedStorageCompatNetwork.Protocol.class)
    void routesRequestsRevisionsAndSnapshotsThroughMatchingProtocol(
            SophisticatedLinkedStorageCompatNetwork.Protocol protocol) throws Exception {
        Map<String, byte[]> classes = compileProtocol(protocol);
        Set<String> loaded = new HashSet<>();
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(ADAPTER) && !name.startsWith("net.p3pp3rf1y.sophisticated")) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (input == null) throw new ClassNotFoundException(name);
                            byte[] bytes = input.readAllBytes();
                            type = defineClass(name, bytes, 0, bytes.length);
                            loaded.add(name);
                        } catch (IOException error) {
                            throw new ClassNotFoundException(name, error);
                        }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                String binary = name.replace('/', '.').replaceFirst("\\.class$", "");
                if (binary.startsWith("net.p3pp3rf1y.sophisticated")) {
                    byte[] bytes = classes.get(binary);
                    return bytes == null ? null : new ByteArrayInputStream(bytes);
                }
                return super.getResourceAsStream(name);
            }
        };

        Class<?> adapter = Class.forName(ADAPTER, true, loader);
        UUID group = UUID.randomUUID();
        // Server snapshots must not link the client-only cache.
        invoke(adapter, "sendSnapshot", new Class<?>[]{ServerLevel.class, ServerPlayer.class, UUID.class},
                null, null, group);
        assertFalse(loaded.contains(protocol.contents));
        Class<?> handler = Class.forName(protocol.handler, true, loader);
        Object snapshot = handler.getField("lastClient").get(null);
        assertEquals(protocol.snapshot, snapshot.getClass().getName());
        assertEquals(group, snapshot.getClass().getMethod("groupId").invoke(snapshot));

        for (long revision : new long[]{-1, 0, 37, Long.MAX_VALUE}) {
            invoke(adapter, "request", new Class<?>[]{UUID.class, long.class}, group, revision);
            Object packet = handler.getField("lastServer").get(null);
            assertEquals(protocol.request, packet.getClass().getName());
            assertEquals(group, packet.getClass().getMethod("groupId").invoke(packet));
            assertEquals(revision, packet.getClass().getMethod("knownRevision").invoke(packet));
        }

        assertEquals(Optional.of(37L), invoke(adapter, "getRevision", new Class<?>[]{UUID.class}, group));
        Class<?> cache = Class.forName(protocol.contents, true, loader);
        assertEquals(group, cache.getField("queried").get(null));
        cache.getField("revision").set(null, Optional.empty());
        assertEquals(Optional.empty(), invoke(adapter, "getRevision", new Class<?>[]{UUID.class}, group));

        ApiShapeProbe.ClassBytes source = classes::get;
        assertEquals(protocol, SophisticatedLinkedStorageCompatNetwork.Protocol.detect(source));
        classes.remove(protocol.request);
        assertThrows(LinkageError.class, () -> SophisticatedLinkedStorageCompatNetwork.Protocol.detect(source));
    }

    private Map<String, byte[]> compileProtocol(SophisticatedLinkedStorageCompatNetwork.Protocol p)
            throws IOException {
        Map<String, String> definitions = Map.of(
                p.request, "public record %s(java.util.UUID groupId, long knownRevision) {}",
                p.snapshot, """
                        public record %s(java.util.UUID groupId) {
                            public static %s createSnapshot(net.minecraft.server.level.ServerLevel level,
                                                           java.util.UUID group) { return new %s(group); }
                        }
                        """,
                p.contents, """
                        public class %s {
                            public static java.util.UUID queried;
                            public static java.util.Optional<Long> revision = java.util.Optional.of(37L);
                            public static java.util.Optional<Long> getRevision(java.util.UUID group) {
                                queried = group; return revision;
                            }
                        }
                        """,
                p.handler, """
                        public class %s {
                            public static final %s INSTANCE = new %s();
                            public static Object lastServer, lastClient;
                            public void sendToServer(Object packet) { lastServer = packet; }
                            public void sendToClient(net.minecraft.server.level.ServerPlayer player, Object packet) {
                                lastClient = packet;
                            }
                        }
                        """
        );
        var arguments = new ArrayList<>(java.util.List.of(
                "-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", temporary.toString()));
        for (var entry : definitions.entrySet()) {
            String binary = entry.getKey();
            int dot = binary.lastIndexOf('.');
            String simple = binary.substring(dot + 1);
            Path file = temporary.resolve(simple + ".java");
            Files.writeString(file, "package " + binary.substring(0, dot) + ";\n"
                    + entry.getValue().formatted(simple, simple, simple));
            arguments.add(file.toString());
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
        Map<String, byte[]> classes = new HashMap<>();
        for (String binary : definitions.keySet()) {
            classes.put(binary, Files.readAllBytes(temporary.resolve(binary.replace('.', '/') + ".class")));
        }
        return classes;
    }

    private static Object invoke(Class<?> owner, String name, Class<?>[] parameters, Object... args)
            throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}
