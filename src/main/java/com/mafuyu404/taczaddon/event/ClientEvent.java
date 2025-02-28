package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.common.AttachmentFromBackpack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
public class ClientEvent {
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        if (event.getMessage().equals("t")) {
//            ArrayList<ItemStack> items = AttachmentFromBackpack.readAllBackpack(Minecraft.getInstance().player);
            System.out.print("\n\n\n\n\n\n\n\neeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n\n\n\n\n\n\n\n\n\n");
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(Minecraft.getInstance().player.getMainHandItem());
            ResourceLocation attachmentId = iAttachment.getAttachmentId(Minecraft.getInstance().player.getMainHandItem());
            System.out.print(attachmentId.toString());
//            backpacks.forEach((s, itemStacks) -> System.out.print(itemStacks.toString()));
        }
    }
}
