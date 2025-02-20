package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.compat.ShoulderSurfingCompat;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Timer;
import java.util.TimerTask;

import static com.tacz.guns.util.InputExtraCheck.isInGame;
import static net.minecraft.client.CameraType.FIRST_PERSON;
import static net.minecraft.client.CameraType.THIRD_PERSON_BACK;

public class BetterAimCamera {
    public static void handle(InputEvent.MouseButton.Post event, KeyMapping AIM_KEY) {
        if (isInGame() && AIM_KEY.matchesMouse(event.getButton())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (IGun.mainhandHoldGun(player)) {
                CameraType CurrentCamera = Minecraft.getInstance().options.getCameraType();
                boolean action;
                if (KeyConfig.HOLD_TO_AIM.get()) {
                    action = event.getAction() != GLFW.GLFW_RELEASE;
                }
                else if (event.getAction() == GLFW.GLFW_PRESS) {
                    action = !operator.isAim();
                }
                else return;
                // IClientPlayerGunOperator.fromLocalPlayer(player).aim(action);
                if (CurrentCamera != FIRST_PERSON) {
                    if (ShoulderSurfingCompat.isShoulderSurfing()) {
                        player.addTag("s2f");
                    }
                    else if (CurrentCamera == THIRD_PERSON_BACK) {
                        player.addTag("t2f");
                    }
                    // 越肩视角会先切视角后转向，切视角会打断转向，如果人物朝前面，准心朝后面，瞄准还是朝前面，因此推迟切视角。
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            Minecraft.getInstance().options.setCameraType(FIRST_PERSON);
                            this.cancel();
                        }
                    },110);
                }
                if (!action) {
                    if (player.getTags().contains("s2f")) {
                        ShoulderSurfingCompat.enableShoulderSurfing();
                        player.removeTag("s2f");
                    }
                    else if (player.getTags().contains("t2f")) {
                        Minecraft.getInstance().options.setCameraType(THIRD_PERSON_BACK);
                        player.removeTag("t2f");
                    }
                }
            }
        }
    }
}
