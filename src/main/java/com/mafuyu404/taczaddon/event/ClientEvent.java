package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.common.AttachmentFromBackpack;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.compat.JeiPlugin;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
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
    public static void onClick(InputEvent.MouseButton event) {
        if (!(Minecraft.getInstance().screen instanceof GunSmithTableScreen)) return;
        if (event.getAction() != InputConstants.RELEASE) return;
        Object data = DataStorage.get("GunSmithTableJEI");
        if (data == null) return;
        ItemStack itemStack = (ItemStack) data;
        if (itemStack.isEmpty()) return;
        JeiPlugin.getJeiRuntime().ifPresent(jeiRuntime -> {
            jeiRuntime.getIngredientManager().getIngredientTypeChecked(itemStack)
                .ifPresent(type -> {
                    DataStorage.set("GunSmithTableJEI", ItemStack.EMPTY);
                    jeiRuntime.getRecipesGui().show(
                            jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, type, itemStack)
                    );
                });
        });
    }
}