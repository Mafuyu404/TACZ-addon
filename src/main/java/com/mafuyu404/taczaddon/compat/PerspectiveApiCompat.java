package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional Perspective API facade used by Leawind Third Person 3.x.
 *
 * The outer class contains no Perspective API types. The inner class is
 * loaded only after ModList confirms the mod is installed.
 */
public final class PerspectiveApiCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LEAWIND_MOD_ID =
            "leawind_third_person";
    private static final String MOD_ID = "perspective_api";

    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED =
            new AtomicBoolean();

    public static final String FIRST_PERSON_ID =
            "perspective_api.first_person";

    public interface PerspectiveApiHandle {
        void restore();
    }

    private PerspectiveApiCompat() {
    }

    public static boolean isInstalled() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    public static String currentNonVanillaPerspectiveId() {
        if (!isUsable()) {
            return null;
        }
        try {
            String perspectiveId =
                    PerspectiveApiCompatInner.currentPerspectiveId();
            if (perspectiveId == null
                    || FIRST_PERSON_ID.equals(perspectiveId)) {
                return null;
            }
            return perspectiveId;
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return null;
        }
    }

    public static boolean isFirstPersonActive() {
        if (!isUsable()) {
            return false;
        }
        try {
            return PerspectiveApiCompatInner.isFirstPersonActive();
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static PerspectiveApiHandle requestFirstPerson() {
        if (!isUsable()) {
            return null;
        }
        try {
            PerspectiveApiHandle unsafeHandle =
                    PerspectiveApiCompatInner.requestFirstPerson();
            if (unsafeHandle == null) {
                return null;
            }
            return new SafePerspectiveApiHandle(unsafeHandle);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return null;
        }
    }

    private static boolean isUsable() {
        ModList modList = ModList.get();
        return modList != null
                && modList.isLoaded(LEAWIND_MOD_ID)
                && modList.isLoaded(MOD_ID)
                && !linkageBroken;
    }

    private static void breakLinkage(LinkageError linkageError) {
        linkageBroken = true;
        if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[TACZ-addon] Perspective API is unavailable; "
                            + "Leawind camera integration disabled",
                    linkageError
            );
        }
    }

    private static final class SafePerspectiveApiHandle
            implements PerspectiveApiHandle {
        private final PerspectiveApiHandle delegate;
        private final AtomicBoolean restored = new AtomicBoolean();

        private SafePerspectiveApiHandle(PerspectiveApiHandle delegate) {
            this.delegate = delegate;
        }

        @Override
        public void restore() {
            if (!this.restored.compareAndSet(false, true)) {
                return;
            }
            try {
                this.delegate.restore();
            } catch (LinkageError linkageError) {
                breakLinkage(linkageError);
            }
        }
    }
}
