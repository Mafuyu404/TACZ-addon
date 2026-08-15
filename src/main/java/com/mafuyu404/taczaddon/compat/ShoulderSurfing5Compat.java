package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSR 5.x-only facade. The outer class never references Shoulder Surfing
 * types so this mod still loads when Shoulder Surfing is absent.
 */
public final class ShoulderSurfing5Compat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "shouldersurfing";

    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED =
            new AtomicBoolean();

    private ShoulderSurfing5Compat() {
    }

    public static boolean isInstalled() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    public static boolean isShoulderSurfing() {
        if (!isUsable()) {
            return false;
        }
        try {
            return ShoulderSurfing5CompatInner.isShoulderSurfing();
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static boolean isFreeLooking() {
        if (!isUsable()) {
            return false;
        }
        try {
            return ShoulderSurfing5CompatInner.isFreeLooking();
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    /**
     * Verifies the exact state produced by the addon when it asks SSR to enter
     * first person. Keeping the SSR query inside this optional boundary means
     * an ABI failure cannot be mistaken for valid camera ownership.
     */
    public static boolean isFirstPersonActive(
            boolean vanillaCameraFirstPerson
    ) {
        if (!isUsable()) {
            return false;
        }
        try {
            return vanillaCameraFirstPerson
                    && !ShoulderSurfing5CompatInner.isShoulderSurfing();
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    /**
     * @return true only if the SSR API call completed successfully.
     */
    public static boolean forceFirstPerson() {
        if (!isUsable()) {
            return false;
        }
        try {
            ShoulderSurfing5CompatInner.forceFirstPerson();
            return true;
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static boolean enableShoulderSurfing() {
        if (!isUsable()) {
            return false;
        }
        try {
            ShoulderSurfing5CompatInner.enableShoulderSurfing();
            return true;
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static boolean showCrosshairWhenShoulderSurfing() {
        if (!isUsable()) {
            return false;
        }
        try {
            return ShoulderSurfing5CompatInner.isShoulderSurfing()
                    && !ShoulderSurfing5CompatInner.isFreeLooking();
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    private static boolean isUsable() {
        return isInstalled() && !linkageBroken;
    }

    private static void breakLinkage(LinkageError linkageError) {
        linkageBroken = true;
        if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[TACZ-addon] SSR 5.x API is unavailable; "
                            + "Shoulder Surfing integration disabled",
                    linkageError
            );
        }
    }
}
