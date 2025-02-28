package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.common.AttachmentFromBackpack;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.init.SyncEvent;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LiberateAttachment.syncRuleWhenLogin(event);
        AttachmentFromBackpack.syncBackpackWhenLogin(event);
    }
    @SubscribeEvent
    public static void onVirtualInventorySetItem(VirtualInventoryChangeEvent.SetItemEvent event) {
//        AttachmentFromBackpack.onAttachmentChange(event);
    }
    @SubscribeEvent
    public static void onVirtualInventoryAdd(VirtualInventoryChangeEvent.AddEvent event) {
//        AttachmentFromBackpack.onAttachmentUnload(event);
    }
}
