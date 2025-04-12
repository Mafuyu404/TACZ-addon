package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.common.AttachmentFromBackpack;
import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void onPlayerLogin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isLocalPlayer()) return;
        LiberateAttachment.syncRuleWhenLogin((ServerPlayer) player);
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
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
//        System.out.print(player.isLocalPlayer()+""+SophisticatedBackpacksCompat.getItemsFromInventoryBackpack(player)+"\n");
        player.getInventory().items.forEach(itemStack -> {
            if (!itemStack.isEmpty()) {
                String [] id = itemStack.getItem().getDescriptionId().split("\\.");
                if (id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack")) {
                    ArrayList<ItemStack> items = new ArrayList<>();
                    BackpackContext.Item context = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, player.getInventory().findSlotMatchingItem(itemStack));
                    Optional<PlayerInventoryHandler> inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(PlayerInventoryProvider.MAIN_INVENTORY);
                    ItemStack stack = inventoryHandler.get().getStackInSlot(player, "", player.getInventory().findSlotMatchingItem(itemStack));
                    InventoryHandler handler = context.getBackpackWrapper(player).getInventoryHandler();
                    int size = itemStack.getTag().getInt("inventorySlots");
                    for (int i = 0; i < size; i++) {
                        ItemStack item = handler.getStackInSlot(i);
                        items.add(item);
                    }
                    System.out.print(player.isLocalPlayer()+""+items+"\n");
                }
            }
        });
    }
}
