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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;
import static net.minecraft.client.CameraType.FIRST_PERSON;

@EventBusSubscriber(
        modid = TACZaddon.MODID,
        value = Dist.CLIENT
)
public final class BetterAimCamera {
    private static final long AIM_CAMERA_SWITCH_DELAY_MS = 110L;

    private static CameraType cameraBeforeAim;

    private static boolean shoulderSurfingBeforeAim;

    private static boolean pendingFirstPersonSwitch;
    private static long pendingSwitchTimeMs;


    private static KeyMapping activeAimKey;

    private BetterAimCamera() {
    }

    public static void handleAfterAimPress(
            InputEvent.MouseButton.Post event,
            KeyMapping aimKey
    ) {
        if (!Config.enableBetterAimCamera()) {
            return;
        }

        if (!isInGame()) {
            return;
        }

        if (!aimKey.matchesMouse(event.getButton())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

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

        boolean holdToAim = KeyConfig.HOLD_TO_AIM.get();
        int mouseAction = event.getAction();

        if (holdToAim) {
            if (mouseAction != GLFW.GLFW_PRESS
                    && mouseAction != GLFW.GLFW_RELEASE) {
                return;
            }
        } else if (mouseAction != GLFW.GLFW_PRESS) {
            return;
        }

        if (!operator.isAim()) {
            restoreCameraAfterAim();
            return;
        }

        CameraType currentCamera =
                minecraft.options.getCameraType();

        if (currentCamera == FIRST_PERSON) {
            return;
        }

        beginDelayedFirstPersonSwitch(
                currentCamera,
                aimKey
        );
    }

    private static void beginDelayedFirstPersonSwitch(
            CameraType currentCamera,
            KeyMapping aimKey
    ) {

        if (cameraBeforeAim == null) {
            cameraBeforeAim = currentCamera;
            shoulderSurfingBeforeAim =
                    ShoulderSurfingCompat.isShoulderSurfing();
        }

        activeAimKey = aimKey;
        pendingFirstPersonSwitch = true;
        pendingSwitchTimeMs =
                Util.getMillis() + AIM_CAMERA_SWITCH_DELAY_MS;
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        if (cameraBeforeAim == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || player.isSpectator()) {
            clearAimCameraState();
            return;
        }

        if (!Config.enableBetterAimCamera()
                || !isInGame()
                || !IGun.mainHandHoldGun(player)) {
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

        if (!pendingFirstPersonSwitch) {
            return;
        }

        if (Util.getMillis() < pendingSwitchTimeMs) {
            return;
        }

        minecraft.options.setCameraType(FIRST_PERSON);

        pendingFirstPersonSwitch = false;
        pendingSwitchTimeMs = 0L;

    }

    private static boolean isAimStillActive(
            IClientPlayerGunOperator operator
    ) {
        if (!operator.isAim()) {
            return false;
        }

        if (!KeyConfig.HOLD_TO_AIM.get()) {
            return true;
        }
        
        return activeAimKey != null
                && activeAimKey.isDown();
    }

    private static void restoreCameraAfterAim() {
        Minecraft minecraft = Minecraft.getInstance();

        pendingFirstPersonSwitch = false;
        pendingSwitchTimeMs = 0L;
        activeAimKey = null;

        if (cameraBeforeAim == null) {
            shoulderSurfingBeforeAim = false;
            return;
        }

        if (shoulderSurfingBeforeAim) {
            ShoulderSurfingCompat.enableShoulderSurfing();
        } else {
            minecraft.options.setCameraType(cameraBeforeAim);
        }

        cameraBeforeAim = null;
        shoulderSurfingBeforeAim = false;
    }

    private static void clearAimCameraState() {
        pendingFirstPersonSwitch = false;
        pendingSwitchTimeMs = 0L;
        activeAimKey = null;

        cameraBeforeAim = null;
        shoulderSurfingBeforeAim = false;
    }
}