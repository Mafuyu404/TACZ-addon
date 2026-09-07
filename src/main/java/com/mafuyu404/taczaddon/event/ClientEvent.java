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

@EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public final class ClientEvent {
    private static VirtualInventory virtualInventory;

    /*
     * Schedules the unconditional world-join backpack contents bootstrap and
     * the later recovery polling/rebuild ticks. The planner tracks player and
     * level identity so old wrapper/virtual inventory state does not leak
     * across reconnects or dimension changes.
     */
    private static final BackpackCacheTickPlanner BACKPACK_CACHE_PLANNER =
            new BackpackCacheTickPlanner();

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

        if (BACKPACK_CACHE_PLANNER.isNewIdentity(
                player,
                player.level()
        )) {
            virtualInventory = null;
        }

        long gameTime = player.level().getGameTime();

        /*
         * TaCZ's ammunition HUD only examines the main-hand gun, so periodic
         * recovery synchronization is only requested while a main-hand gun is
         * present. The unconditional bootstrap request above does not depend
         * on holdingGun: a player must never need to open a backpack or equip
         * a gun before TACZAddon requests authoritative backpack contents.
         */
        boolean holdingGun =
                IGun.getIGunOrNull(player.getMainHandItem()) != null;

        switch (BACKPACK_CACHE_PLANNER.tick(
                player,
                player.level(),
                holdingGun,
                gameTime
        )) {
            case BOOTSTRAP_REQUEST -> {
                /*
                 * Request-only tick. The server response is asynchronous, so
                 * the virtual inventory must not be built from an
                 * unsynchronized client wrapper yet.
                 */
                SophisticatedBackpacksCompat.syncAllBackpack(player);
                virtualInventory = null;
            }
            case PERIODIC_SYNC_REQUEST -> {
                /*
                 * Recovery only: ordinary consumption correctness comes from
                 * the immediate server contents payload + cache invalidation.
                 */
                SophisticatedBackpacksCompat.syncAllBackpack(player);
            }
            case REBUILD_CACHE -> {
                refreshBackpackCache(player);
            }
            case WAIT -> {
                // Nothing to do this tick.
            }
        }
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
        BACKPACK_CACHE_PLANNER.invalidateCache();
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
        BACKPACK_CACHE_PLANNER.reset();
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
