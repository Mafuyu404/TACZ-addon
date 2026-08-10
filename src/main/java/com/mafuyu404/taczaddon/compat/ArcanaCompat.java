package com.mafuyu404.taczaddon.compat;

import net.minecraftforge.fml.ModList;

import java.util.function.Predicate;

/**
 * Central optional-compatibility probe for TaCZ: Arcana.
 *
 * <p>No Arcana type is linked from this class. The class-name check keeps an
 * incomplete or older installation fail-closed while leaving the normal
 * TACZ-addon packet path untouched.</p>
 */
public final class ArcanaCompat {
    private static final String MOD_ID = "taczexpands";
    private static final String TACZ_ADDON_COMPAT_CLASS =
            "group.taczexpands.server.compat.taczaddon.TACZAddonCompat";

    private ArcanaCompat() {
    }

    public static boolean canHandleNativeAttachmentMessages() {
        return canHandleNativeAttachmentMessages(
                ModList.get().isLoaded(MOD_ID),
                ArcanaCompat::isClassPresent
        );
    }

    static boolean canHandleNativeAttachmentMessages(
            boolean modLoaded,
            Predicate<String> classProbe
    ) {
        return modLoaded
                && classProbe.test(TACZ_ADDON_COMPAT_CLASS);
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(
                    className,
                    false,
                    ArcanaCompat.class.getClassLoader()
            );
            return true;
        } catch (ClassNotFoundException
                 | LinkageError
                 | SecurityException ignored) {
            return false;
        }
    }
}
