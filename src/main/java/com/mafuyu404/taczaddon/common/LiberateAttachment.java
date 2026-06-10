package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.*;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.item.AttachmentItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.List;

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
            items.addAll(AttachmentItem.fillItemCategory(type));
        });
        return items;
    }
    public static Inventory useVirtualInventory(Inventory inventory) {
        List<ItemStack> AttachmentItems = getAttachmentItems();
        if (!isLiberated(inventory.player)) {
            AttachmentItems = filterAttachmentItem(AttachmentItems, GunSmithingManager.getResult(inventory.getSelected()));
            if (AttachmentItems.isEmpty()) return inventory;
        }

        int size = AttachmentItems.size() + 10;
        VirtualInventory virtualInventory = new VirtualInventory(size, inventory.player);

        for (int i = 0; i < size - 1; i++) {
            if (i < 9) virtualInventory.setItem(i, inventory.getSelected());
            else virtualInventory.setItem(i, AttachmentItems.get(i - 9));
        }

        return virtualInventory;
    }

    public static void onRuleChange(MinecraftServer server, GameRules.BooleanValue value) {
        server.getPlayerList().getPlayers().forEach(player -> {
            boolean liberateAttachment = player.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT);
            NetworkHandler.sendToClient(player, new PrimitivePacket("gamerule.liberateAttachment", liberateAttachment));
            boolean showAttachmentDetail = player.level().getGameRules().getBoolean(RuleRegistry.SHOW_ATTACHMENT_DETAIL);
            NetworkHandler.sendToClient(player, new PrimitivePacket("gamerule.showAttachmentDetail", showAttachmentDetail));
        });
    }
    public static void syncRuleWhenLogin(ServerPlayer serverPlayer) {
        boolean liberateAttachment = serverPlayer.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT);
        NetworkHandler.sendToClient(serverPlayer, new PrimitivePacket("gamerule.liberateAttachment", liberateAttachment));
        boolean showAttachmentDetail = serverPlayer.level().getGameRules().getBoolean(RuleRegistry.SHOW_ATTACHMENT_DETAIL);
        NetworkHandler.sendToClient(serverPlayer, new PrimitivePacket("gamerule.showAttachmentDetail", showAttachmentDetail));
    }

    public static boolean isLiberated(Player player) {
        Object liberateAttachment = DataStorage.get("gamerule.liberateAttachment");
        boolean gamerule = liberateAttachment != null && (boolean) liberateAttachment;
        return gamerule || player.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT);
    }

    public static List<ItemStack> filterAttachmentItem(List<ItemStack> all, List<String> list) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack itemStack : all) {
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(itemStack);
            if (iAttachment != null && list.contains(iAttachment.getAttachmentId(itemStack).toString())) {
                result.add(itemStack);
            }
        }
        return result;
    }
}
