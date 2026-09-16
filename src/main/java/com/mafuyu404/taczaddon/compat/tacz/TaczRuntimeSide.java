package com.mafuyu404.taczaddon.compat.tacz;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Side a version adapter is allowed to run on.
 *
 * <p>The Mixin configuration already routes client-only adapters, but the
 * runtime gate must report the same distinction so a disabled adapter is never
 * logged as merely "bundled".
 */
public enum TaczRuntimeSide {
    COMMON,
    CLIENT,
    SERVER;

    public boolean allows(Dist current) {
        if (current == null) {
            /*
             * Outside a Forge launch (unit tests, tooling) the side is
             * unknown; do not turn that into a false "missing" verdict.
             */
            return true;
        }
        return switch (this) {
            case COMMON -> true;
            case CLIENT -> current == Dist.CLIENT;
            case SERVER -> current == Dist.DEDICATED_SERVER;
        };
    }

    /**
     * @return the current distribution, or null when Forge has not selected
     *         one yet
     */
    public static Dist currentDist() {
        try {
            return FMLEnvironment.dist;
        } catch (RuntimeException | LinkageError unavailable) {
            return null;
        }
    }
}
