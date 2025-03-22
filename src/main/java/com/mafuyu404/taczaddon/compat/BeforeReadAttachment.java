//package com.mafuyu404.taczaddon.compat;
//
//import com.mafuyu404.taczaddon.TACZaddon;
//import com.tacz.guns.api.item.IAttachment;
//import com.tacz.guns.api.item.IGun;
//import com.tacz.guns.client.animation.screen.RefitTransform;
//import dev.latvian.mods.kubejs.event.EventJS;
//import dev.latvian.mods.kubejs.typings.Info;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.world.entity.player.Inventory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.fml.common.Mod;
//
//import java.util.ArrayList;
//
//@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = TACZaddon.MODID)
//public class BeforeReadAttachment extends EventJS {
//    ArrayList<ItemStack> result = new ArrayList<>();
//    Player player;
//
//    public BeforeReadAttachment(Player _player) {
//        player = _player;
//        result.addAll(player.getInventory().items);
//    }
//
//    @Info("Who need to read attachments.")
//    public Player getPlayer() {
//        return player;
//    }
//    @Info("Include all attachments in inventory.")
//    public ArrayList<ItemStack> getInventoryAttachments() {
//        ArrayList<ItemStack> inventoryAttachments = new ArrayList<>();
//        Inventory inventory = player.getInventory();
//        for (int i = 0; i < inventory.getContainerSize(); i++) {
//            ItemStack inventoryItem = inventory.getItem(i);
//            IAttachment attachment = IAttachment.getIAttachmentOrNull(inventoryItem);
//            IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
//            if (attachment != null && iGun != null && attachment.getType(inventoryItem) == RefitTransform.getCurrentTransformType() && iGun.allowAttachment(player.getMainHandItem(), inventoryItem)) {
//                inventoryAttachments.add(inventory.getItem(i).copy());
//            }
//        }
//        return inventoryAttachments;
//    }
//    @Info("Default value is all items in inventory. But you can change it for result.")
//    public ArrayList<ItemStack> getResultAttachments() {
//        return result;
//    }
//}
