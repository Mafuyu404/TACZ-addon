package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.TACZaddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void onPlayerLogin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        LiberateAttachment.syncRuleWhenLogin(serverPlayer);
//        AttachmentFromBackpack.syncBackpackWhenLogin(event);
    }
    @SubscribeEvent
    public static void onVirtualInventorySetItem(VirtualInventoryChangeEvent.SetItemEvent event) {
//        AttachmentFromBackpack.onAttachmentChange(event);
    }
    @SubscribeEvent
    public static void onVirtualInventoryAdd(VirtualInventoryChangeEvent.AddEvent event) {
//        AttachmentFromBackpack.onAttachmentUnload(event);
    }
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (FMLEnvironment.dist.isClient()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            System.out.print(player.getUUID()+"\n");
        }
    }
//    @SubscribeEvent
//    public static void jump(LivingEvent.LivingJumpEvent event) {
//        if (!(event.getEntity() instanceof Player player)) return;
//        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
//            BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, index);
//            BackpackContainer container = new BackpackContainer(player.containerMenu.containerId + 1, player, backpackContext);
//            int size = backpack.getTag().getInt("inventorySlots");
//            container.realInventorySlots.forEach(slot -> {
//                if (slot.index < size)
//            });
//            System.out.print(container.realInventorySlots.size()+"\n");
//            return false;
//        });
//    }
}
