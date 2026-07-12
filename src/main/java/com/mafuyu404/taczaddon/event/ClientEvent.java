package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksClientCompat;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.KeyBindings;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.network.SwitchGunPacket;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client features unrelated to server-authoritative gunsmith positioning.
 *
 * The obsolete RightClickBlock workbench-position capture is intentionally
 * absent. Gunsmith positions come only from the server block entity session.
 */
@Mod.EventBusSubscriber(
        modid = TACZaddon.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientEvent {
    private ClientEvent() {
    }

    @SubscribeEvent
    public static void renderSophisticatedItemRelations(
            ContainerScreenEvent.Render.Foreground event
    ) {
        SophisticatedBackpacksClientCompat.renderItemRelations(event);
    }

    @SubscribeEvent
    public static void storeGunList(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null
                || event.getKey()
                != KeyBindings.SWITCH_GUN_KEY.getKey().getValue()
                || event.getAction() != GLFW.GLFW_PRESS
                || minecraft.player == null) {
            return;
        }

        ItemStack heldGun =
                minecraft.player.getMainHandItem();
        if (IGun.getIGunOrNull(heldGun) == null) {
            return;
        }

        ArrayList<String> gunIds = new ArrayList<>();
        Inventory inventory =
                minecraft.player.getInventory();

        for (int slot = 0;
             slot < inventory.getContainerSize();
             slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (IGun.getIGunOrNull(stack) != null) {
                taczaddon$getGunId(stack).ifPresent(gunIds::add);
            }
        }

        if (gunIds.size() > 1) {
            DataStorage.set("storeGunList", gunIds);
        }
    }

    @SubscribeEvent
    public static void switchGun(
            InputEvent.MouseScrollingEvent event
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null
                || !KeyBindings.SWITCH_GUN_KEY.isDown()) {
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

        Object stored = DataStorage.get("storeGunList");
        if (!(stored instanceof List<?> rawList)) {
            return;
        }

        ArrayList<String> gunIds = new ArrayList<>();
        for (Object entry : rawList) {
            if (entry instanceof String gunId) {
                gunIds.add(gunId);
            }
        }

        if (gunIds.size() <= 1) {
            return;
        }

        int targetIndex = gunIds.lastIndexOf(currentGunId);
        if (targetIndex < 0) {
            return;
        }

        boolean forward = event.getScrollDelta() < 0.0D;
        targetIndex = forward
                ? (targetIndex + 1) % gunIds.size()
                : (targetIndex - 1 + gunIds.size()) % gunIds.size();

        int targetSlot = -1;
        Inventory inventory = player.getInventory();

        for (int index = 0;
             index < inventory.getContainerSize();
             index++) {
            int slot = forward
                    ? index
                    : inventory.getContainerSize() - 1 - index;

            if (taczaddon$getGunId(inventory.getItem(slot))
                    .filter(gunIds.get(targetIndex)::equals)
                    .isPresent()) {
                targetSlot = slot;
                break;
            }
        }

        if (targetSlot < 0) {
            return;
        }

        NetworkHandler.CHANNEL.sendToServer(
                new SwitchGunPacket(targetSlot)
        );
        event.setCanceled(true);
    }

    /*
     * This cache belongs to the separate backpack ammo/HUD compatibility
     * feature. It is intentionally not used by gunsmith-table crafting.
     */
    public static VirtualInventory _virtualInventory;

    @SubscribeEvent
    public static void storageBackpack(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            _virtualInventory = null;
            return;
        }

        if (DataStorage.get("backpackData") == null) {
            SophisticatedBackpacksCompat.syncAllBackpack(player);
            DataStorage.set("backpackData", true);
        }

        ArrayList<ItemStack> combined =
                SophisticatedBackpacksCompat
                        .getItemsFromInventoryBackpack(player);
        combined.addAll(player.getInventory().items);

        VirtualInventory virtualInventory =
                new VirtualInventory(combined.size(), player);

        for (int index = 0;
             index < combined.size();
             index++) {
            virtualInventory.setItem(index, combined.get(index));
        }

        _virtualInventory = virtualInventory;
    }

    private static Optional<String> taczaddon$getGunId(
            ItemStack itemStack
    ) {
        if (itemStack.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag tag = itemStack.getTag();
        if (tag == null) {
            return Optional.empty();
        }

        String gunId = tag.getString("GunId");
        return gunId.isEmpty()
                ? Optional.empty()
                : Optional.of(gunId);
    }
}
