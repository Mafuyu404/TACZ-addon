package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.VirtualContainerLoader;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleContainerReader(ArrayList<ItemStack> items) {
        if (Minecraft.getInstance().screen instanceof GunSmithTableScreen screen
                && screen instanceof VirtualContainerLoader loader) {
            loader.taczaddon$setVirtualContainer(items);
            screen.updateIngredientCount();
        }
    }

    public static void handleRefreshRefitScreen() {
        Minecraft minecraft = Minecraft.getInstance();

        LocalPlayer player = minecraft.player;

        if (player == null) {
            return;
        }

        if (!(minecraft.screen instanceof GunRefitScreen screen)) {
            return;
        }

        screen.init();

        AttachmentPropertyManager.postChangeEvent(
                player,
                player.getMainHandItem()
        );
    }
}
