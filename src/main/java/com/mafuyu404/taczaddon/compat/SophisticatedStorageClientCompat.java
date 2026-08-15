package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@OnlyIn(Dist.CLIENT)
public final class SophisticatedStorageClientCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BACKPACKS_MOD_ID =
            "sophisticatedbackpacks";
    private static final String STORAGE_MOD_ID =
            "sophisticatedstorage";

    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED =
            new AtomicBoolean();

    private SophisticatedStorageClientCompat() {
    }

    public static boolean isAnySophisticatedStorageLoaded() {
        ModList modList = ModList.get();
        return modList != null
                && (modList.isLoaded(BACKPACKS_MOD_ID)
                || modList.isLoaded(STORAGE_MOD_ID));
    }

    public static boolean isStorageScreen(
            AbstractContainerScreen<?> screen
    ) {
        if (!isUsable()) {
            return false;
        }
        try {
            return SophisticatedStorageClientCompatInner
                    .isStorageScreen(screen);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static void renderItemRelations(
            ContainerScreenEvent.Render.Foreground event
    ) {
        if (!isUsable()) {
            return;
        }
        try {
            SophisticatedStorageClientCompatInner
                    .renderItemRelations(event);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
        }
    }

    private static boolean isUsable() {
        return isAnySophisticatedStorageLoaded()
                && !linkageBroken;
    }

    private static void breakLinkage(LinkageError linkageError) {
        linkageBroken = true;
        if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[TACZ-addon] Sophisticated Storage/Core API is "
                            + "unavailable; storage highlighting disabled",
                    linkageError
            );
        }
    }
}
