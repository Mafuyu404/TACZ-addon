package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.network.VirtualAttachmentRefitPacket;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.refit.InventoryAttachmentSlot;
import com.tacz.guns.client.gui.components.refit.RefitTurnPageButton;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = GunRefitScreen.class,
        remap = false
)
public abstract class GunRefitScreenMixin
        extends Screen {

    @Unique
    private static final int
            TACZADDON_ATTACHMENTS_PER_PAGE = 8;

    @Unique
    private static final int
            TACZADDON_SLOT_SIZE = 18;

    @Shadow
    private int currentPage;

    protected GunRefitScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "addInventoryAttachmentButtons",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void taczaddon$addSafeAttachmentButtons(
            CallbackInfo ci
    ) {
        ci.cancel();

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        AttachmentType selectedType =
                RefitTransform.getCurrentTransformType();

        if (selectedType == AttachmentType.NONE) {
            return;
        }

        Inventory realInventory =
                minecraft.player.getInventory();

        Inventory displayedInventory =
                LiberateAttachment.useVirtualInventory(
                        realInventory
                );

        boolean virtualInventory =
                displayedInventory
                        instanceof VirtualInventory;

        int startX = this.width - 30;
        int startY = 50;

        int pageStart =
                this.currentPage
                        * TACZADDON_ATTACHMENTS_PER_PAGE;

        int compatibleCount = 0;
        int currentY = startY;

        ItemStack gunStack =
                minecraft.player.getMainHandItem();

        IGun gun = IGun.getIGunOrNull(gunStack);

        if (gun == null) {
            return;
        }

        for (int slot = 0;
             slot < displayedInventory.getContainerSize();
             slot++) {

            ItemStack attachmentStack =
                    displayedInventory.getItem(slot);

            IAttachment attachment =
                    IAttachment.getIAttachmentOrNull(
                            attachmentStack
                    );

            if (attachment == null) {
                continue;
            }

            if (attachment.getType(attachmentStack)
                    != selectedType) {
                continue;
            }

            if (!gun.allowAttachment(
                    gunStack,
                    attachmentStack
            )) {
                continue;
            }

            compatibleCount++;

            if (compatibleCount <= pageStart) {
                continue;
            }

            if (compatibleCount
                    > pageStart
                    + TACZADDON_ATTACHMENTS_PER_PAGE) {
                continue;
            }

            int capturedSlot = slot;

            InventoryAttachmentSlot button =
                    new InventoryAttachmentSlot(
                            startX,
                            currentY,
                            capturedSlot,
                            displayedInventory,
                            pressedButton -> {
                                ItemStack clickedStack =
                                        displayedInventory
                                                .getItem(
                                                        capturedSlot
                                                );

                                IAttachment clickedAttachment =
                                        IAttachment
                                                .getIAttachmentOrNull(
                                                        clickedStack
                                                );

                                if (clickedAttachment == null) {
                                    return;
                                }

                                ResourceLocation attachmentId =
                                        clickedAttachment
                                                .getAttachmentId(
                                                        clickedStack
                                                );

                                if (attachmentId == null) {
                                    return;
                                }

                                SoundPlayManager.playerRefitSound(
                                        clickedStack,
                                        minecraft.player,
                                        SoundManager.INSTALL_SOUND
                                );

                                int sourceSlot =
                                        virtualInventory
                                                ? VirtualAttachmentRefitPacket
                                                  .VIRTUAL_SOURCE_SLOT
                                                : capturedSlot;

                                NetworkHandler.sendToServer(
                                        new VirtualAttachmentRefitPacket(
                                                sourceSlot,
                                                realInventory.selected,
                                                attachmentId
                                        )
                                );
                            }
                    );

            this.addRenderableWidget(button);

            currentY += TACZADDON_SLOT_SIZE;
        }

        int totalPages =
                compatibleCount == 0
                        ? 0
                        : (compatibleCount - 1)
                          / TACZADDON_ATTACHMENTS_PER_PAGE;

        if (this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }

        if (this.currentPage > 0) {
            this.addRenderableWidget(
                    new RefitTurnPageButton(
                            startX,
                            startY - 10,
                            true,
                            button -> {
                                if (this.currentPage > 0) {
                                    this.currentPage--;
                                    this.init();
                                }
                            }
                    )
            );
        }

        if (this.currentPage < totalPages) {
            this.addRenderableWidget(
                    new RefitTurnPageButton(
                            startX,
                            startY
                                    + TACZADDON_SLOT_SIZE
                                    * TACZADDON_ATTACHMENTS_PER_PAGE
                                    + 2,
                            false,
                            button -> {
                                if (this.currentPage
                                        < totalPages) {
                                    this.currentPage++;
                                    this.init();
                                }
                            }
                    )
            );
        }
    }
}