package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.client.GunRefitScreenAccess;
import com.mafuyu404.taczaddon.common.LiberateAttachmentService;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

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

    @ModifyArg(
            method = "addInventoryAttachmentButtons()V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tacz/guns/client/gui/components/refit/"
                                    + "InventoryAttachmentSlot;"
                                    + "<init>(IIILnet/minecraft/world/"
                                    + "entity/player/Inventory;"
                                    + "Lnet/minecraft/client/gui/components/"
                                    + "Button$OnPress;)V",
                    remap = false
            ),
            index = 4,
            remap = false,
            require = 1
    )
    private Button.OnPress taczaddon$wrapInventoryAttachmentButton(
            Button.OnPress originalOnPress
    ) {
        return new Button.OnPress() {
            @Override
            public void onPress(Button button) {
                if (!ClientSyncedConfig.liberateAttachment()) {
                    originalOnPress.onPress(button);
                    return;
                }

                if (button instanceof InventoryAttachmentSlot attachmentSlot) {
                    Inventory exactInventory =
                            ((InventoryAttachmentSlotAccess) attachmentSlot)
                                    .taczaddon$getInventory();
                    taczaddon$sendAttachmentIdInsteadOfVirtualSlot(
                            exactInventory,
                            button
                    );
                }
            }
        };
    }

    private static void taczaddon$sendAttachmentIdInsteadOfVirtualSlot(
            Inventory inventory,
            Button button
    ) {
        LocalPlayer player = inventory.player instanceof LocalPlayer value
                ? value
                : null;
        if (player == null) {
            return;
        }

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
                        inventory.selected,
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
