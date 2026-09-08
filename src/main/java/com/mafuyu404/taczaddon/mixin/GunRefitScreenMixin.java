package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.common.VirtualAttachmentData;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.network.VirtualAttachmentRefitPacket;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.refit.GunAttachmentSlot;
import com.tacz.guns.client.gui.components.refit.HSVSliderGroup;
import com.tacz.guns.client.gui.components.refit.InventoryAttachmentSlot;
import com.tacz.guns.client.gui.components.refit.RefitTurnPageButton;
import com.tacz.guns.client.gui.components.refit.RefitUnloadButton;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side GunRefitScreen integration.
 *
 * <p>Two stable source-method boundaries are taken over, never synthetic
 * lambdas:
 * <ul>
 *     <li>{@code addAttachmentTypeButtons()V} — rebuilt so the unload button
 *     does not demand main-inventory space for virtual attachments.</li>
 *     <li>{@code addInventoryAttachmentButtons()V} — rebuilt to show the
 *     virtual attachment inventory.</li>
 * </ul>
 *
 * <p>The rebuild of {@code addAttachmentTypeButtons} mirrors the TaCZ 1.21.1
 * source (layout, NONE handling, transform switching, laser HSV controls).
 * The only intended difference is the unload button gating:
 * {@code VirtualAttachmentData.isVirtual(...) == true} attachments may be
 * unloaded even with a full main inventory. The client is only a UI gate; the
 * server re-reads the authoritative gun state in
 * {@code AttachmentRefitService.unload(...)}.
 */
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

    @Inject(method = "init", at = @At("HEAD"), require = 1)
    private void taczaddon$refreshExternalCandidates(CallbackInfo ci) {
        com.mafuyu404.taczaddon.client.RefitExternalSourceState.requestIfNeeded();
    }

    /**
     * Version-bound takeover of {@code addAttachmentTypeButtons()V}.
     *
     * <p>This is a source mirror of the current TaCZ 1.21.1 method (verified
     * against the actual dependency jar), and is far more stable than the
     * previous {@code lambda$addAttachmentTypeButtons$14} redirect, whose
     * synthetic numbering could change between builds.
     */
    @Inject(
            method = "addAttachmentTypeButtons()V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void taczaddon$rebuildAttachmentTypeButtons(
            CallbackInfo ci
    ) {
        ci.cancel();

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        ItemStack gunStack = player.getMainHandItem();

        IGun gun = IGun.getIGunOrNull(gunStack);

        if (gun == null) {
            return;
        }

        int startX = this.width - 30;
        int y = 10;

        Inventory inventory = player.getInventory();

        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                if (RefitTransform.getCurrentTransformType()
                        == AttachmentType.NONE) {
                    TimelessAPI.getGunDisplay(gunStack)
                            .map(GunDisplayInstance::getLaserConfig)
                            .ifPresent(laserConfig ->
                                    taczaddon$addLaserControls(
                                            laserConfig,
                                            inventory,
                                            AttachmentType.NONE
                                    )
                            );
                }
                continue;
            }

            GunAttachmentSlot slot =
                    new GunAttachmentSlot(
                            startX,
                            y,
                            type,
                            inventory.selected,
                            inventory,
                            this::taczaddon$onTypeButtonPressed
                    );

            if (RefitTransform.getCurrentTransformType() == type) {
                slot.setSelected(true);

                RefitUnloadButton unloadButton =
                        new RefitUnloadButton(
                                startX + 5,
                                y + 18 + 2,
                                button ->
                                        taczaddon$onUnloadPressed(
                                                slot,
                                                inventory,
                                                player,
                                                button
                                        )
                        );

                ItemStack attachedItem =
                        slot.getAttachmentItem();

                if (!attachedItem.isEmpty()) {
                    this.addRenderableWidget(unloadButton);

                    Item item = attachedItem.getItem();

                    if (item instanceof IAttachment attachment) {
                        ResourceLocation attachmentId =
                                attachment.getAttachmentId(
                                        attachedItem
                                );

                        if (attachmentId != null) {
                            TimelessAPI
                                    .getClientAttachmentIndex(
                                            attachmentId
                                    )
                                    .map(ClientAttachmentIndex
                                            ::getLaserConfig)
                                    .ifPresent(laserConfig ->
                                            taczaddon$addLaserControls(
                                                    laserConfig,
                                                    inventory,
                                                    type
                                            )
                                    );
                        }
                    }
                }
            }

            this.addRenderableWidget(slot);

            startX -= 18;
        }
    }

    /**
     * Mirror of TaCZ's per-type button press handler
     * ({@code lambda$addAttachmentTypeButtons$13} logic in the current jar):
     * blocked types switch to the overview, selected types switch back to the
     * overview, everything else switches to the requested type.
     */
    @Unique
    private void taczaddon$onTypeButtonPressed(Button button) {
        if (!(button instanceof GunAttachmentSlot slot)) {
            return;
        }

        AttachmentType type = slot.getType();

        if (!slot.isAllow()) {
            if (RefitTransform.changeRefitScreenView(
                    AttachmentType.NONE
            )) {
                this.init();
            }
            return;
        }

        if (RefitTransform.getCurrentTransformType() == type
                && type != AttachmentType.NONE) {
            if (RefitTransform.changeRefitScreenView(
                    AttachmentType.NONE
            )) {
                this.init();
            }
            return;
        }

        if (RefitTransform.changeRefitScreenView(type)) {
            this.init();
        }
    }

    /**
     * Mirror of TaCZ's unload button handler
     * ({@code lambda$addAttachmentTypeButtons$14} logic in the current jar),
     * with one intended difference: virtual attachments skip the
     * {@code inventory.getFreeSlot()} requirement.
     *
     * <p>This is client-side UI gating only. Ownership is decided again on
     * the server from the authoritative gun stack.
     */
    @Unique
    private void taczaddon$onUnloadPressed(
            GunAttachmentSlot slot,
            Inventory inventory,
            LocalPlayer player,
            Button button
    ) {
        ItemStack attachedItem = slot.getAttachmentItem();

        if (attachedItem.isEmpty()) {
            return;
        }

        boolean virtual =
                VirtualAttachmentData.isVirtual(attachedItem);

        if (virtual || inventory.getFreeSlot() != -1) {
            SoundPlayManager.playerRefitSound(
                    attachedItem,
                    player,
                    SoundManager.UNINSTALL_SOUND
            );

            NetworkHandler.sendToServer(
                    new ClientMessageUnloadAttachment(
                            inventory.selected,
                            RefitTransform.getCurrentTransformType()
                    )
            );
            return;
        }

        player.sendSystemMessage(
                Component.translatable(
                        "gui.tacz.gun_refit.unload.no_space"
                )
        );
    }

    /**
     * Mirror of TaCZ's laser HSV control block
     * ({@code lambda$addAttachmentTypeButtons$12/$15} logic in the current
     * jar).
     */
    @Unique
    private void taczaddon$addLaserControls(
            LaserConfig laserConfig,
            Inventory inventory,
            AttachmentType type
    ) {
        if (!laserConfig.canEdit()) {
            return;
        }

        HSVSliderGroup group =
                new HSVSliderGroup(
                        this.width - 140,
                        this.height - 64,
                        120,
                        16,
                        inventory,
                        inventory.selected,
                        type
                );

        this.addRenderableWidget(group.getHueSlider());
        this.addRenderableWidget(group.getSaturationSlider());
    }

    /**
     * Existing addon takeover of the attachment inventory buttons; unchanged
     * from the previous round.
     */
    @Inject(
            method = "addInventoryAttachmentButtons()V",
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

        boolean virtualInventory = LiberateAttachment.isLiberated(minecraft.player);
        Inventory displayedInventory = virtualInventory
                ? LiberateAttachment.useVirtualInventory(realInventory)
                : com.mafuyu404.taczaddon.client.RefitExternalSourceState.createDisplayInventory(realInventory);

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

                                if (displayedInventory instanceof com.mafuyu404.taczaddon.client.RefitDisplayInventory display) {
                                    var external = display.externalAt(capturedSlot);
                                    if (external != null) {
                                        NetworkHandler.sendToServer(new com.mafuyu404.taczaddon.network.RefitExternalAttachmentInstallPacket(
                                                realInventory.selected, external.locator(), external.attachmentId(), external.type()));
                                        return;
                                    }
                                }

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
