package com.mafuyu404.taczaddon.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public final class SophisticatedBackpacksClientCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";

    private SophisticatedBackpacksClientCompat() {
    }

    public static boolean isStorageScreen(AbstractContainerScreen<?> screen) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }

        return SophisticatedBackpacksClientCompatInner.isStorageScreen(screen);
    }

    public static void renderItemRelations(ContainerScreenEvent.Render.Foreground event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        SophisticatedBackpacksClientCompatInner.renderItemRelations(event);
    }
}
