package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.ClientSessionState;
import com.mafuyu404.taczaddon.init.KeyBindings;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.init.ItemStackData;
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
public class ClientEvent {
    private static final int BACKPACK_CACHE_REFRESH_TICKS = 20;
    private static VirtualInventory virtualInventory;
    private static long nextBackpackRefreshTick;
    private static UUID cachedPlayerId;

    @SubscribeEvent
    public static void onVirtualInventoryAdd(PlayerInteractEvent.RightClickBlock event) {
        ClientSessionState.setLastGunSmithInteractPos(event.getHitVec().getBlockPos());
    }

    @SubscribeEvent
    public static void onGame(RenderFrameEvent.Post event) {
        // Reserved render-frame hook for client-only features.
    }

    @SubscribeEvent
    public static void storeGunList(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (event.getKey() != KeyBindings.SWITCH_GUN_KEY.getKey().getValue() || event.getAction() != GLFW.GLFW_PRESS) return;
        if (mc.player == null) return;

        ItemStack gunItem = mc.player.getMainHandItem();
        if (IGun.getIGunOrNull(gunItem) == null) return;
        ArrayList<String> gunList = new ArrayList<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (IGun.getIGunOrNull(itemStack) != null) {
                taczaddon$getGunId(itemStack).ifPresent(gunList::add);
            }
        }
        if (gunList.size() <= 1) return;
        ClientSessionState.setGunSwitchList(gunList);
    }

    @SubscribeEvent
    public static void switchGun(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) return;
        if (!KeyBindings.SWITCH_GUN_KEY.isDown()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack gunItem = player.getMainHandItem();
        if (IGun.getIGunOrNull(gunItem) == null) return;
        String currentGun = taczaddon$getGunId(gunItem).orElse(null);
        if (currentGun == null) return;

        List<String> gunList = ClientSessionState.getGunSwitchList();
        if (gunList.size() <= 1) return;

        double scrollDelta = event.getScrollDeltaY();
        int index = gunList.lastIndexOf(currentGun);
        if (index == -1) return;
        if (scrollDelta < 0) index = (index == gunList.size() - 1) ? 0 : index + 1;
        else index = (index == 0) ? gunList.size() - 1 : index - 1;
        int slot = -1;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            int targetIndex = (scrollDelta < 0) ? i : inventory.getContainerSize() - 1 - i;
            ItemStack itemStack = inventory.getItem(targetIndex);
            if (taczaddon$getGunId(itemStack).filter(gunList.get(index)::equals).isPresent()) slot = targetIndex;
        }
        if (slot == -1) return;
        NetworkHandler.sendToServer(new SwitchGunPacket(slot));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void storageBackpack(ClientTickEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            clearClientCaches();
            return;
        }

        UUID playerId = player.getUUID();
        if (!playerId.equals(cachedPlayerId)) {
            cachedPlayerId = playerId;
            virtualInventory = null;
            nextBackpackRefreshTick = 0L;
            ClientSessionState.setBackpackDataSynced(false);
        }

        if (!ClientSessionState.isBackpackDataSynced()) {
            SophisticatedBackpacksCompat.syncAllBackpack(player);
            ClientSessionState.setBackpackDataSynced(true);
        }

        long gameTime = player.level().getGameTime();
        if (virtualInventory != null && gameTime < nextBackpackRefreshTick) {
            return;
        }

        refreshBackpackCache(player);
        nextBackpackRefreshTick = gameTime + BACKPACK_CACHE_REFRESH_TICKS;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientCaches();
        ClientSessionState.clear();
    }

    public static Optional<Inventory> getVirtualInventory() {
        return Optional.ofNullable(virtualInventory);
    }

    private static void refreshBackpackCache(Player player) {
        List<ItemStack> backpack = new ArrayList<>(SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(player));
        backpack.addAll(player.getInventory().items);
        VirtualInventory updatedInventory = new VirtualInventory(backpack.size(), player);
        for (int i = 0; i < backpack.size(); i++) {
            updatedInventory.setItem(i, backpack.get(i));
        }
        virtualInventory = updatedInventory;
    }

    private static void clearClientCaches() {
        virtualInventory = null;
        nextBackpackRefreshTick = 0L;
        cachedPlayerId = null;
        ClientSessionState.setBackpackDataSynced(false);
    }

    private static Optional<String> taczaddon$getGunId(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return Optional.empty();
        }
        String gunId = ItemStackData.getCustomDataCopy(itemStack).getString("GunId");
        return gunId.isEmpty() ? Optional.empty() : Optional.of(gunId);
    }
}
