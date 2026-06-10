package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.ShoulderSurfingCompat;
import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;
import static net.minecraft.client.CameraType.FIRST_PERSON;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID, value = Dist.CLIENT)
public class BetterAimCamera {
    private static final long AIM_CAMERA_SWITCH_DELAY_MS = 110L;

    private static CameraType cameraBeforeAim = null;
    private static boolean shoulderSurfingBeforeAim = false;

    private static boolean pendingFirstPersonSwitch = false;
    private static long pendingSwitchTimeMs = 0L;
    private static KeyMapping pendingAimKey = null;

    public static void handle(InputEvent.MouseButton.Post event, KeyMapping aimKey) {
        if (!Config.enableBetterAimCamera()) return;
        if (!isInGame()) return;
        if (!aimKey.matchesMouse(event.getButton())) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || player.isSpectator()) {
            clearAimCameraState();
            return;
        }

        if (!(player instanceof IClientPlayerGunOperator operator)) {
            clearAimCameraState();
            return;
        }

        if (!IGun.mainHandHoldGun(player)) {
            clearAimCameraState();
            return;
        }

        boolean action;

        if (KeyConfig.HOLD_TO_AIM.get()) {
            action = event.getAction() != GLFW.GLFW_RELEASE;
        } else if (event.getAction() == GLFW.GLFW_PRESS) {
            action = !operator.isAim();
        } else {
            return;
        }

        if (!action) {
            restoreCameraAfterAim();
            return;
        }

        CameraType currentCamera = mc.options.getCameraType();

        if (currentCamera == FIRST_PERSON) {
            return;
        }

        beginDelayedFirstPersonSwitch(currentCamera, aimKey);
    }

    private static void beginDelayedFirstPersonSwitch(CameraType currentCamera, KeyMapping aimKey) {
        if (cameraBeforeAim == null) {
            cameraBeforeAim = currentCamera;
            shoulderSurfingBeforeAim = ShoulderSurfingCompat.isShoulderSurfing();
        }

        pendingFirstPersonSwitch = true;
        pendingSwitchTimeMs = Util.getMillis() + AIM_CAMERA_SWITCH_DELAY_MS;
        pendingAimKey = aimKey;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!pendingFirstPersonSwitch) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || player.isSpectator()) {
            clearAimCameraState();
            return;
        }

        if (!Config.enableBetterAimCamera() || !isInGame() || !IGun.mainHandHoldGun(player)) {
            restoreCameraAfterAim();
            return;
        }

        if (!(player instanceof IClientPlayerGunOperator operator)) {
            restoreCameraAfterAim();
            return;
        }

        if (!isAimStillActive(operator)) {
            restoreCameraAfterAim();
            return;
        }

        if (Util.getMillis() < pendingSwitchTimeMs) {
            return;
        }

        mc.options.setCameraType(FIRST_PERSON);

        pendingFirstPersonSwitch = false;
        pendingAimKey = null;
    }

    private static boolean isAimStillActive(IClientPlayerGunOperator operator) {
        if (KeyConfig.HOLD_TO_AIM.get()) {
            return pendingAimKey != null && pendingAimKey.isDown();
        }

        return operator.isAim();
    }

    private static void restoreCameraAfterAim() {
        Minecraft mc = Minecraft.getInstance();

        pendingFirstPersonSwitch = false;
        pendingSwitchTimeMs = 0L;
        pendingAimKey = null;

        if (cameraBeforeAim == null) {
            shoulderSurfingBeforeAim = false;
            return;
        }

        if (shoulderSurfingBeforeAim) {
            ShoulderSurfingCompat.enableShoulderSurfing();
        } else {
            mc.options.setCameraType(cameraBeforeAim);
        }

        cameraBeforeAim = null;
        shoulderSurfingBeforeAim = false;
    }

    private static void clearAimCameraState() {
        pendingFirstPersonSwitch = false;
        pendingSwitchTimeMs = 0L;
        pendingAimKey = null;
        cameraBeforeAim = null;
        shoulderSurfingBeforeAim = false;
    }
}
