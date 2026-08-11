package com.mafuyu404.taczaddon.testutil;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

public final class MinecraftTestBootstrap {
    private MinecraftTestBootstrap() {
    }

    public static void prepare() throws Exception {
        SharedConstants.tryDetectVersion();

        Field bootstrapped = null;
        for (String fieldName : new String[] {
                "f_135867_",
                "isBootstrapped"
        }) {
            try {
                bootstrapped = Bootstrap.class.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException ignored) {
                // Try the alternate runtime field name.
            }
        }

        if (bootstrapped == null) {
            throw new NoSuchFieldException(
                    "Bootstrap bootstrapped flag"
            );
        }

        bootstrapped.setAccessible(true);
        bootstrapped.setBoolean(null, true);
        ItemStack.EMPTY.isEmpty();
        Bootstrap.bootStrap();
    }
}
