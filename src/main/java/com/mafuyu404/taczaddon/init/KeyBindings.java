package com.mafuyu404.taczaddon.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping SWITCH_GUN_KEY = new KeyMapping("key.taczaddon.switch_gun.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM, // 按键类型（键盘/鼠标）
            GLFW.GLFW_KEY_TAB, // 默认绑定到 TAB 键
            "key.categories.taczaddon" // 分类（需对应语言文件）
    );
}
