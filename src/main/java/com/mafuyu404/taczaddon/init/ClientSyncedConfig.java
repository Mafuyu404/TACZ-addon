package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.network.ServerFeatureConfigSyncPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client mirror of policies owned by the connected server.
 *
 * Server-authoritative features must remain disabled until an explicit
 * configuration packet has been accepted from the current connection.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSyncedConfig {
    private static volatile int batchCraftMax = 1;

    /*
     * Fail closed. A previous server's value must never authorize client
     * prediction on a new connection.
     */
    private static volatile boolean enableShootWhileReloading =
            false;

    private ClientSyncedConfig() {
    }

    public static void apply(
            ServerFeatureConfigSyncPacket packet
    ) {
        batchCraftMax = packet.batchCraftMax();
        enableShootWhileReloading =
                packet.enableShootWhileReloading();
    }

    public static int batchCraftMax() {
        return batchCraftMax;
    }

    public static boolean enableShootWhileReloading() {
        return enableShootWhileReloading;
    }

    public static void resetToSafeDefaults() {
        batchCraftMax = 1;
        enableShootWhileReloading = false;
    }
}