package com.mafuyu404.taczaddon.compat;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SophisticatedLinkedStorageIsolationTest {
    private static final String FACADE =
            "com.mafuyu404.taczaddon.compat.SophisticatedLinkedStorageCompat";

    @Test
    void absentLinkedApiLeavesOrdinaryClassificationAvailable() throws Exception {
        Class<?> facade = Class.forName(FACADE, true, isolatedLoader(true));
        assertEquals(false, invoke(facade, "supported", new Class<?>[0]));
        Object resolution = invoke(facade, "resolve", new Class<?>[] {ItemStack.class}, (Object) null);
        assertEquals(false, invoke(resolution.getClass(), resolution, "linked", new Class<?>[0]));
        assertDoesNotThrow(() -> invoke(facade, "requestSnapshot",
                new Class<?>[] {UUID.class}, UUID.randomUUID()));
    }

    @Test
    void missingLinkedMessageDoesNotEscapeOrBreakOrdinaryClassification() throws Exception {
        Class<?> facade = Class.forName(FACADE, true, isolatedLoader(false));
        assertEquals(true, invoke(facade, "supported", new Class<?>[0]));
        assertDoesNotThrow(() -> invoke(facade, "requestSnapshot",
                new Class<?>[] {UUID.class}, UUID.randomUUID()));
        Object resolution = invoke(facade, "resolve", new Class<?>[] {ItemStack.class}, (Object) null);
        assertEquals(false, invoke(resolution.getClass(), resolution, "linked", new Class<?>[0]));
    }

    private static ClassLoader isolatedLoader(boolean oldGeneration) {
        return new ClassLoader(SophisticatedLinkedStorageIsolationTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (oldGeneration && name.startsWith("net.p3pp3rf1y.sophisticatedcore.linkedstorage.")) {
                    throw new ClassNotFoundException(name);
                }
                if (name.endsWith(".RequestLinkedStorageBackpackContentsMessage")) {
                    throw new ClassNotFoundException(name);
                }
                if (!name.startsWith(FACADE)) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        if (oldGeneration && name.equals(FACADE + "326")) {
                            fail("Old generations must never load the linked implementation");
                        }
                        try (var stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (stream == null) throw new ClassNotFoundException(name);
                            byte[] bytes = stream.readAllBytes();
                            loaded = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException error) {
                            throw new ClassNotFoundException(name, error);
                        }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
    }

    private static Object invoke(Class<?> owner, String name, Class<?>[] parameters, Object... args)
            throws Exception {
        return invoke(owner, null, name, parameters, args);
    }

    private static Object invoke(Class<?> owner, Object instance, String name,
                                 Class<?>[] parameters, Object... args) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }
}
