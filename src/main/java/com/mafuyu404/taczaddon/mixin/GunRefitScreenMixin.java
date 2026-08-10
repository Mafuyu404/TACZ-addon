package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.client.GunRefitScreenAccess;
import com.mafuyu404.taczaddon.common.LiberateAttachmentService;
import com.mafuyu404.taczaddon.compat.ArcanaCompat;
import com.mafuyu404.taczaddon.init.ClientSyncedConfig;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.LiberateAttachmentInstallPacket;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.refit.InventoryAttachmentSlot;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only hooks for TaCZ 1.1.8-hotfix (Curse file 8141310).
 */
@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class GunRefitScreenMixin
        implements GunRefitScreenAccess {
    @Shadow
    private int currentPage;

    @Redirect(
            method = "addInventoryAttachmentButtons()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    remap = true
            ),
            require = 1,
            remap = false
    )
    private Inventory taczaddon$attachmentCatalogInventory(
            LocalPlayer player
    ) {
        Inventory realInventory = player.getInventory();
        return LiberateAttachmentService.createInventory(
                realInventory,
                ClientSyncedConfig.liberateAttachment()
        );
    }

    @Redirect(
            method = "addAttachmentTypeButtons()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    remap = true
            ),
            require = 1,
            remap = false
    )
    private Inventory taczaddon$attachmentControlsInventory(
            LocalPlayer player
    ) {
        Inventory realInventory = player.getInventory();
        return LiberateAttachmentService.createInventory(
                realInventory,
                ClientSyncedConfig.liberateAttachment()
        );
    }

    @Inject(
            method = "lambda$addInventoryAttachmentButtons$9",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void taczaddon$sendAttachmentIdInsteadOfVirtualSlot(
            Inventory inventory,
            LocalPlayer player,
            Button button,
            CallbackInfo callback
    ) {
        if (!ClientSyncedConfig.liberateAttachment()) {
            return;
        }

        // Arcana owns the complete attachment transaction behind TaCZ's
        // native packet. Its server-side handler is still gated by our
        // earlier-running validation mixin.
        if (ArcanaCompat.canHandleNativeAttachmentMessages()) {
            return;
        }

        callback.cancel();
        if (!(button instanceof InventoryAttachmentSlot attachmentSlot)) {
            return;
        }

        int slot = attachmentSlot.getSlotIndex();
        if (!LiberateAttachmentService.isValidIndex(inventory, slot)) {
            return;
        }

        ItemStack candidate = inventory.getItem(slot);
        IAttachment attachment =
                IAttachment.getIAttachmentOrNull(candidate);
        if (attachment == null
                || attachment.getType(candidate) == AttachmentType.NONE) {
            return;
        }

        ResourceLocation attachmentId =
                attachment.getAttachmentId(candidate);
        if (attachmentId == null) {
            return;
        }

        SoundPlayManager.playerRefitSound(
                candidate,
                player,
                SoundManager.INSTALL_SOUND
        );
        NetworkHandler.CHANNEL.sendToServer(
                new LiberateAttachmentInstallPacket(
                        player.getInventory().selected,
                        attachmentId
                )
        );
    }

    @Override
    public void taczaddon$rebuildLiberatedAttachmentButtons() {
        this.currentPage = 0;
        ((GunRefitScreen) (Object) this).init();
    }
}
