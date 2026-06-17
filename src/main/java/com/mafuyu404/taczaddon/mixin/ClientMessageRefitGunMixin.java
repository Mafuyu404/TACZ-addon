//package com.mafuyu404.taczaddon.mixin;
//
//import com.mafuyu404.taczaddon.common.LiberateAttachment;
//import com.mafuyu404.taczaddon.init.VirtualInventory;
//import com.tacz.guns.network.message.ClientMessageRefitGun;
//import net.minecraft.world.entity.player.Inventory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//import org.spongepowered.asm.mixin.injection.Redirect;
//
//@Mixin(
//        value = ClientMessageRefitGun.class,
//        remap = false
//)
//public abstract class ClientMessageRefitGunMixin {
//
//    /**
//     * Supplies generated/unlocked attachment choices to TaCZ.
//     *
//     * The server independently reconstructs this inventory, so it does not
//     * trust attachment ItemStack data supplied by the client.
//     */
//    @ModifyVariable(
//            method = "lambda$handle$0",
//            at = @At("STORE"),
//            ordinal = 0
//    )
//    private static Inventory taczaddon$useVirtualInventory(
//            Inventory realInventory
//    ) {
//        return LiberateAttachment.useVirtualInventory(
//                realInventory
//        );
//    }
//
//    @Redirect(
//            method = "lambda$handle$0",
//            at = @At(
//                    value = "INVOKE",
//                    target =
//                            "Lnet/minecraft/world/entity/player/Inventory;"
//                                    + "setItem("
//                                    + "ILnet/minecraft/world/item/ItemStack;"
//                                    + ")V"
//            )
//    )
//    private static void taczaddon$returnReplacedAttachment(
//            Inventory inventory,
//            int slot,
//            ItemStack oldAttachment
//    ) {
//        /*
//         * Preserve normal TaCZ behavior when it is operating on the real
//         * inventory.
//         */
//        if (!(inventory instanceof VirtualInventory)) {
//            inventory.setItem(slot, oldAttachment);
//            return;
//        }
//
//        if (oldAttachment.isEmpty()) {
//            return;
//        }
//
//        Player player = inventory.player;
//        Inventory realInventory = player.getInventory();
//
//        ItemStack remainder = oldAttachment.copy();
//
//        /*
//         * Inventory#add mutates remainder, leaving only the part that could
//         * not be inserted.
//         */
//        realInventory.add(remainder);
//
//        if (!remainder.isEmpty()) {
//            player.drop(remainder, false);
//        }
//
//        realInventory.setChanged();
//    }
//}