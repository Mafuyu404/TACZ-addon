package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.network.SwitchGunPacket;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public final class ClientEvent {
    /*
     * Request fresh Sophisticated Backpacks inventory NBT once per second
     * while the player is holding a gun.
     *
     * The request transfers complete backpack inventory and upgrade NBT, so it
     * should not run every client tick.
     */
    private static final int BACKPACK_SYNC_INTERVAL_TICKS = 20;

    /*
     * Rebuild the synthetic inventory periodically even when no payload has
     * explicitly invalidated it. This also keeps normal player inventory
     * contents reasonably current for consumers of the virtual inventory.
     */
    private static final int BACKPACK_CACHE_REFRESH_TICKS = 20;

    private static VirtualInventory virtualInventory;

    private static long nextBackpackSyncTick;
    private static long nextBackpackRefreshTick;

    private static UUID cachedPlayerId;

    private ClientEvent() {
    }

    @SubscribeEvent
    public static void onVirtualInventoryAdd(
            PlayerInteractEvent.RightClickBlock event
    ) {
        ClientSessionState.setLastGunSmithInteractPos(
                event.getHitVec().getBlockPos()
        );
    }

    @SubscribeEvent
    public static void onGame(RenderFrameEvent.Post event) {
        // Reserved render-frame hook for client-only features.
    }

    @SubscribeEvent
    public static void storeGunList(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null) {
            return;
        }

        if (event.getKey()
                != KeyBindings.SWITCH_GUN_KEY.getKey().getValue()) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack heldGun = player.getMainHandItem();
        if (IGun.getIGunOrNull(heldGun) == null) {
            return;
        }

        List<String> gunList = new ArrayList<>();
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (IGun.getIGunOrNull(stack) != null) {
                taczaddon$getGunId(stack).ifPresent(gunList::add);
            }
        }

        if (gunList.size() <= 1) {
            return;
        }

        ClientSessionState.setGunSwitchList(gunList);
    }

    @SubscribeEvent
    public static void switchGun(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null) {
            return;
        }

        if (!KeyBindings.SWITCH_GUN_KEY.isDown()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack heldGun = player.getMainHandItem();
        if (IGun.getIGunOrNull(heldGun) == null) {
            return;
        }

        String currentGunId =
                taczaddon$getGunId(heldGun).orElse(null);

        if (currentGunId == null) {
            return;
        }

        List<String> gunList =
                ClientSessionState.getGunSwitchList();

        if (gunList.size() <= 1) {
            return;
        }

        int currentIndex = gunList.lastIndexOf(currentGunId);
        if (currentIndex < 0) {
            return;
        }

        double scrollDelta = event.getScrollDeltaY();

        int targetGunIndex;
        if (scrollDelta < 0) {
            targetGunIndex =
                    currentIndex == gunList.size() - 1
                            ? 0
                            : currentIndex + 1;
        } else {
            targetGunIndex =
                    currentIndex == 0
                            ? gunList.size() - 1
                            : currentIndex - 1;
        }

        String targetGunId = gunList.get(targetGunIndex);
        Inventory inventory = player.getInventory();

        int targetSlot = findGunSlot(
                inventory,
                targetGunId,
                scrollDelta < 0
        );

        if (targetSlot < 0) {
            return;
        }

        NetworkHandler.sendToServer(
                new SwitchGunPacket(targetSlot)
        );

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void storageBackpack(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) {
            clearClientCaches();
            return;
        }

        UUID playerId = player.getUUID();

        if (!playerId.equals(cachedPlayerId)) {
            cachedPlayerId = playerId;

            virtualInventory = null;
            nextBackpackSyncTick = 0L;
            nextBackpackRefreshTick = 0L;
        }

        long gameTime = player.level().getGameTime();

        /*
         * TaCZ's ammunition HUD only examines the main-hand gun, so full
         * backpack synchronization is only requested while a main-hand gun is
         * present.
         */
        boolean holdingGun =
                IGun.getIGunOrNull(player.getMainHandItem()) != null;

        if (holdingGun && gameTime >= nextBackpackSyncTick) {
            SophisticatedBackpacksCompat.syncAllBackpack(player);

            nextBackpackSyncTick =
                    gameTime + BACKPACK_SYNC_INTERVAL_TICKS;
        }

        if (virtualInventory != null
                && gameTime < nextBackpackRefreshTick) {
            return;
        }

        refreshBackpackCache(player);

        nextBackpackRefreshTick =
                gameTime + BACKPACK_CACHE_REFRESH_TICKS;
    }

    @SubscribeEvent
    public static void onClientLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        clearClientCaches();
        ClientSessionState.clear();
    }

    public static Optional<Inventory> getVirtualInventory() {
        return Optional.ofNullable(virtualInventory);
    }

    /**
     * Called after a Sophisticated Backpacks contents payload updates
     * BackpackStorage and refreshes the corresponding BackpackWrapper.
     *
     * The next client tick will rebuild the virtual inventory immediately.
     */
    public static void invalidateBackpackCache() {
        virtualInventory = null;
        nextBackpackRefreshTick = 0L;
    }

    private static void refreshBackpackCache(Player player) {
        List<ItemStack> combinedInventory = new ArrayList<>(
                SophisticatedBackpacksCompat
                        .getItemsFromInventoryBackpack(player)
        );

        /*
         * The backpack items are inserted first to preserve the existing
         * virtual-inventory ordering. TaCZ only counts ammunition, so the
         * ordering does not affect the displayed total.
         */
        combinedInventory.addAll(player.getInventory().items);

        VirtualInventory updatedInventory =
                new VirtualInventory(
                        combinedInventory.size(),
                        player
                );

        for (int slot = 0;
             slot < combinedInventory.size();
             slot++) {
            updatedInventory.setItem(
                    slot,
                    combinedInventory.get(slot)
            );
        }

        virtualInventory = updatedInventory;
    }

    private static int findGunSlot(
            Inventory inventory,
            String targetGunId,
            boolean searchForward
    ) {
        for (int offset = 0;
             offset < inventory.getContainerSize();
             offset++) {
            int slot = searchForward
                    ? offset
                    : inventory.getContainerSize() - 1 - offset;

            ItemStack stack = inventory.getItem(slot);

            if (taczaddon$getGunId(stack)
                    .filter(targetGunId::equals)
                    .isPresent()) {
                return slot;
            }
        }

        return -1;
    }

    private static void clearClientCaches() {
        virtualInventory = null;

        nextBackpackSyncTick = 0L;
        nextBackpackRefreshTick = 0L;

        cachedPlayerId = null;
    }

    private static Optional<String> taczaddon$getGunId(
            ItemStack itemStack
    ) {
        if (itemStack.isEmpty()) {
            return Optional.empty();
        }

        String gunId = ItemStackData
                .getCustomDataCopy(itemStack)
                .getString("GunId");

        return gunId.isEmpty()
                ? Optional.empty()
                : Optional.of(gunId);
    }
}