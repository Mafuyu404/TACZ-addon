package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.ClientSyncedConfig;
import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.ItemIconToast;
import com.mafuyu404.taczaddon.init.crafting.GunSmithCraftScreenAccess;
import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import com.mafuyu404.taczaddon.network.GunSmithCraftResultPacket;
import com.mafuyu404.taczaddon.network.GunSmithSourceSnapshotPacket;
import com.mafuyu404.taczaddon.network.ServerFeatureConfigSyncPacket;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGunSmithPacketHandler {
    private ClientGunSmithPacketHandler() {
    }

    public static void handleConfigSync(
            ServerFeatureConfigSyncPacket message
    ) {
        ClientSyncedConfig.apply(message);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen
                instanceof GunSmithSourceScreenAccess sourceAccess) {
            sourceAccess.taczaddon$requestSourceRefresh();
        }
    }

    public static void handleSourceSnapshot(
            GunSmithSourceSnapshotPacket message
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GunSmithTableScreen screen)
                || !(screen
                instanceof GunSmithSourceScreenAccess sourceAccess)) {
            return;
        }

        sourceAccess.taczaddon$acceptSourceSnapshot(
                message.containerId(),
                message.requestId(),
                message.sourceRevision(),
                message.externalStacks()
        );
    }

    public static void handleCraftResult(
            GunSmithCraftResultPacket message
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GunSmithTableScreen screen)
                || !(screen
                instanceof GunSmithCraftScreenAccess craftAccess)) {
            return;
        }

        boolean accepted = craftAccess.taczaddon$acceptCraftResult(
                message.containerId(),
                message.requestId(),
                message.success(),
                message.craftedExecutions(),
                message.outputPerCraft(),
                message.failureReason()
        );

        if (!accepted) {
            return;
        }

        if (message.success()
                && message.craftedExecutions() > 0
                && Config.enableGunSmithTableCraftToast()) {
            ItemStack output = message.outputPerCraft();
            int totalCount = output.getCount()
                    * message.craftedExecutions();

            Component title = Component.translatable(
                    "gui.taczaddon.gun_smith_table.crafted",
                    message.craftedExecutions()
            );
            Component description = output
                    .getHoverName()
                    .copy()
                    .append(Component.literal(" × " + totalCount));

            ItemIconToast.show(
                    title,
                    description,
                    output
            );
        }

        if (screen instanceof GunSmithSourceScreenAccess sourceAccess) {
            sourceAccess.taczaddon$requestSourceRefresh();
        }
    }
}
