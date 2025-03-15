package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.client.ClientDataStorage;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.item.AttachmentItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;

public class LiberateAttachment {
    public static ArrayList<ItemStack> getAttachmentItems() {
        ArrayList<ItemStack> items = new ArrayList<>();
        ArrayList<AttachmentType> types = new ArrayList<>();
        types.add(AttachmentType.SCOPE);
        types.add(AttachmentType.MUZZLE);
        types.add(AttachmentType.STOCK);
        types.add(AttachmentType.GRIP);
        types.add(AttachmentType.LASER);
        types.add(AttachmentType.EXTENDED_MAG);
        types.forEach(type -> {
            //                System.out.print("\n");
            //                System.out.print(itemStack.getDescriptionId());
            items.addAll(AttachmentItem.fillItemCategory(type));
        });
        return items;
    }
    public static Inventory useVirtualInventory(Inventory inventory) {
//        System.out.print("\n");
//        System.out.print(FMLEnvironment.dist);
        Object gamerule = ClientDataStorage.get("gamerule.liberateAttachment");
        if ((boolean) gamerule) {
            ArrayList<ItemStack> AttachmentItems = getAttachmentItems();
            int size = AttachmentItems.size() + 9;
            VirtualInventory virtualInventory = new VirtualInventory(size, inventory.player);
            for (int i = 0; i < size; i++) {
                if (i < 9) virtualInventory.setItem(i, inventory.getSelected());
                else virtualInventory.setItem(i, AttachmentItems.get(i - 9));
            }
            return virtualInventory;
        }
        else return inventory;
//        else return AttachmentFromBackpack.useVirtualInventory(inventory);
    }
    public static void onRuleChange(MinecraftServer server, GameRules.BooleanValue value) {
        server.getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.sendToClient(player, new PrimitivePacket("gamerule.liberateAttachment", value.get()));
        });
    }
    public static void syncRuleWhenLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        boolean value = serverPlayer.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT);
        NetworkHandler.sendToClient(serverPlayer, new PrimitivePacket("gamerule.liberateAttachment", value));
    }
}
