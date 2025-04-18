package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class GunRefitScreenMixin {
    @ModifyVariable(method = "addInventoryAttachmentButtons", at = @At("STORE"), ordinal = 0)
    private Inventory modifyInventory(Inventory inventory) {
        Object liberateAttachment = DataStorage.get("gamerule.liberateAttachment");
        boolean gamerule = liberateAttachment != null && (boolean) liberateAttachment;
        if (gamerule || inventory.player.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT)) {
            return LiberateAttachment.useVirtualInventory(inventory);
        }
        else {
            return inventory;
        }
    }
}
