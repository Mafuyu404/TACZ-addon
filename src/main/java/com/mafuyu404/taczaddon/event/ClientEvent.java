package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.JeiCompat;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.KeyBindings;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.SwitchGunPacket;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    public static void onVirtualInventoryAdd(PlayerInteractEvent.RightClickBlock event) {
        DataStorage.set("BetterGunSmithTable.interactBlockPos", event.getHitVec().getBlockPos());
    }
    @SubscribeEvent
    public static void onGame(TickEvent.RenderTickEvent event) {
//        LuaAnimationStateMachine<GunAnimationStateContext> animationStateMachine = (LuaAnimationStateMachine<GunAnimationStateContext>) DataStorage.get("animationStateMachine");
//        if (animationStateMachine != null) {
//            ObjectAnimation animation = animationStateMachine.getAnimationController().getAnimation(4).getAnimation();
//            if (animation.name.contains("reload")) {
//                float maxEndTimeS = animationStateMachine.getAnimationController().getAnimation(4).getAnimation().getMaxEndTimeS();
//                long processNs = animationStateMachine.getAnimationController().getAnimation(4).getProgressNs();
//                float process = Math.round(processNs / 1e7) * 0.01f;
//                float maxEndTime = Math.round(maxEndTimeS * 100) * 0.01f;
//                if (process != maxEndTime) {
//                    float reloadSpeedIncrease = 0;
//                    animationStateMachine.getContext().adjustAnimationProgress(4, 0.016F * reloadSpeedIncrease, false);
//                }
//            }
//        }
    }
    @SubscribeEvent
    public static void JEIRecipes(InputEvent.MouseButton event) {
        if (Minecraft.getInstance().screen == null) return;
        if (!(Minecraft.getInstance().screen instanceof GunSmithTableScreen)) return;
        if (event.getAction() != InputConstants.RELEASE) return;
        Object data = DataStorage.get("GunSmithTableJEI");
        if (data == null) return;
        ItemStack itemStack = (ItemStack) data;
        if (itemStack.isEmpty()) return;
        boolean result = JeiCompat.showRecipes(itemStack);
        if (result) DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void storeGunList(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (event.getKey() != KeyBindings.SWITCH_GUN_KEY.getKey().getValue() || event.getAction() != GLFW.GLFW_PRESS) return;
        ItemStack gunItem = mc.player.getMainHandItem();
        if (IGun.getIGunOrNull(gunItem) == null) return;
        ArrayList<String> GunList = new ArrayList<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (IGun.getIGunOrNull(inventory.getItem(i)) != null) GunList.add(inventory.getItem(i).getTag().getString("GunId"));
        }
        if (GunList.size() <= 1) return;
        DataStorage.set("storeGunList", GunList);
    }
    @SubscribeEvent
    public static void switchGun(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) return;
        if (!KeyBindings.SWITCH_GUN_KEY.isDown()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack gunItem = player.getMainHandItem();
        if (IGun.getIGunOrNull(gunItem) == null) return;
        Object data = DataStorage.get("storeGunList");
        if (data == null) return;
        ArrayList<String> GunList = (ArrayList<String>) data;
        int index = GunList.lastIndexOf(gunItem.getTag().getString("GunId"));
        if (index == -1) return;
        if (event.getScrollDelta() < 0) index = (index == GunList.size() - 1) ? 0 : index + 1;
        else index = (index == 0) ? GunList.size() - 1 : index - 1;
        int slot = -1;
        System.out.print(index);
        System.out.print("\n");
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            int _i = (event.getScrollDelta() < 0) ? i : inventory.getContainerSize() - 1 - i;
            ItemStack itemStack = inventory.getItem(_i);
            if (!itemStack.isEmpty() && inventory.getItem(_i).getTag() != null) {
                if (inventory.getItem(_i).getTag().getString("GunId").equals(GunList.get(index))) slot = _i;
            }
        }
        if (slot == -1) return;
        NetworkHandler.CHANNEL.sendToServer(new SwitchGunPacket(slot));
        event.setCanceled(true);
    }
    @SubscribeEvent
    public static void ammoBox(InputEvent.MouseButton event) {
//        Minecraft mc = Minecraft.getInstance();
//        if (!(Minecraft.getInstance().screen instanceof InventoryScreen)) return;
//        if (event.getAction() != InputConstants.RELEASE) return;
//        if (event.getButton() != 1) return;
//        System.out.print(event.getButton()+"\n");
//        if (!mc.options.keyShift.isDown()) return;

//        if (event.getButton() != InputConstants.RELEASE) return;
    }
}