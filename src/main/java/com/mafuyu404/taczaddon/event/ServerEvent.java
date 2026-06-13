package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void onPlayerLogin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        LiberateAttachment.syncRuleWhenLogin(serverPlayer);
    }
    @SubscribeEvent
    public static void onVirtualInventorySetItem(VirtualInventoryChangeEvent.SetItemEvent event) {
        // Intentionally inactive; backpack writes are not mirrored through virtual inventory events.
    }
    @SubscribeEvent
    public static void onVirtualInventoryAdd(VirtualInventoryChangeEvent.AddEvent event) {
        // Intentionally inactive; unloaded attachments remain in the current inventory flow.
    }
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Reserved server-login hook; rule sync is handled from EntityJoinLevelEvent.
    }
}
