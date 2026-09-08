package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.network.*;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class GunSmithCraftBridgeState {
    private static long nextRequestId;
    private static final Map<Long, Integer> pending = new LinkedHashMap<>();
    private GunSmithCraftBridgeState() {}
    public static int requestedCount(boolean shift) { return shift ? ClientSyncedConfig.getBatchCraftMax() : 1; }
    public static void request(int containerId, ResourceLocation recipeId, int requested) {
        if (pending.size() >= 16) pending.remove(pending.keySet().iterator().next());
        long requestId = ++nextRequestId;
        pending.put(requestId, containerId);
        NetworkHandler.sendToServer(new GunSmithCraftRequestPacket(containerId, requestId, recipeId, requested));
    }
    public static void accept(GunSmithCraftResultPacket result) {
        Integer container = pending.remove(result.requestId());
        if (container == null || container != result.containerId()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.containerMenu.containerId != result.containerId()) return;
        if (minecraft.screen instanceof GunSmithTableScreen screen) screen.updateIngredientCount();
        if (!result.success() || result.craftedExecutions() <= 0 || result.outputPerCraft().isEmpty()) return;
        var output = result.outputPerCraft();
        long total = (long) output.getCount() * result.craftedExecutions();
        if (Config.enableGunSmithTableCraftToast()) {
            ItemIconToast.create("Crafted", output.getHoverName().getString() + " x " + total, output.copy());
        }
    }
    public static void reset() { pending.clear(); nextRequestId = 0; }
}
