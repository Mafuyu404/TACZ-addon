package com.mafuyu404.taczaddon.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public final class SophisticatedStorageClientCompat {
    private static final String BACKPACKS_MOD_ID =
            "sophisticatedbackpacks";
    private static final String STORAGE_MOD_ID =
            "sophisticatedstorage";

    private SophisticatedStorageClientCompat() {
    }

    /*
     * Outer facade: no Sophisticated API type is referenced here. The inner
     * class may only be loaded after one of the storage mods is present.
     */
    public static boolean isAnySophisticatedStorageLoaded() {
        return ModList.get().isLoaded(BACKPACKS_MOD_ID)
                || ModList.get().isLoaded(STORAGE_MOD_ID);
    }

    public static boolean isStorageScreen(AbstractContainerScreen<?> screen) {
        if (!isAnySophisticatedStorageLoaded()) {
            return false;
        }

        return SophisticatedStorageClientCompatInner.isStorageScreen(screen);
    }

    public static void renderItemRelations(ContainerScreenEvent.Render.Foreground event) {
        if (!isAnySophisticatedStorageLoaded()) {
            return;
        }

        SophisticatedStorageClientCompatInner.renderItemRelations(event);
    }
}
