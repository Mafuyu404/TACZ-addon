package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
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
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    public static void onVirtualInventoryAdd(PlayerInteractEvent.RightClickBlock event) {
        DataStorage.set("BetterGunSmithTable.interactBlockPos", event.getHitVec().getBlockPos());
    }
    @SubscribeEvent
    public static void onGame(TickEvent.RenderTickEvent event) {
        // Reserved render-tick hook for client-only features.
    }

    @SubscribeEvent
    public static void storeGunList(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (event.getKey() != KeyBindings.SWITCH_GUN_KEY.getKey().getValue() || event.getAction() != GLFW.GLFW_PRESS) return;
        if (mc.player == null) return;

        ItemStack gunItem = mc.player.getMainHandItem();
        if (IGun.getIGunOrNull(gunItem) == null) return;
        ArrayList<String> GunList = new ArrayList<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (IGun.getIGunOrNull(itemStack) != null) {
                taczaddon$getGunId(itemStack).ifPresent(GunList::add);
            }
        }
        if (GunList.size() <= 1) return;
        DataStorage.set("storeGunList", GunList);
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

        Object data = DataStorage.get("storeGunList");
        if (!(data instanceof List<?> storedList)) return;

        ArrayList<String> GunList = new ArrayList<>();
        for (Object entry : storedList) {
            if (entry instanceof String gunId) {
                GunList.add(gunId);
            }
        }
        if (GunList.size() <= 1) return;

        int index = GunList.lastIndexOf(currentGun);
        if (index == -1) return;
        if (event.getScrollDelta() < 0) index = (index == GunList.size() - 1) ? 0 : index + 1;
        else index = (index == 0) ? GunList.size() - 1 : index - 1;
        int slot = -1;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            int _i = (event.getScrollDelta() < 0) ? i : inventory.getContainerSize() - 1 - i;
            ItemStack itemStack = inventory.getItem(_i);
            if (taczaddon$getGunId(itemStack).filter(GunList.get(index)::equals).isPresent()) slot = _i;
        }
        if (slot == -1) return;
        NetworkHandler.CHANNEL.sendToServer(new SwitchGunPacket(slot));
        event.setCanceled(true);
    }
    public static VirtualInventory _virtualInventory = null;

    @SubscribeEvent
    public static void storageBackpack(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (DataStorage.get("backpackData") == null) {
            SophisticatedBackpacksCompat.syncAllBackpack(player);
            DataStorage.set("backpackData", true);
        }
        ArrayList<ItemStack> backpack = SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(player);
        backpack.addAll(player.getInventory().items);
        VirtualInventory virtualInventory = new VirtualInventory(backpack.size(), player);
        for (int i = 0; i < backpack.size(); i++) {
            virtualInventory.setItem(i, backpack.get(i));
        }
        _virtualInventory = virtualInventory;
    }

    private static Optional<String> taczaddon$getGunId(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (itemStack.isEmpty() || tag == null) {
            return Optional.empty();
        }

        String gunId = tag.getString("GunId");
        return gunId.isEmpty() ? Optional.empty() : Optional.of(gunId);
    }
}
